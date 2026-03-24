package main

import (
	"context"
	"log"
	"net"
	"net/http"
	"os"
	"os/signal"
	"strings"
	"syscall"
	"time"
)

var secretKey []byte

func main() {
	log.SetFlags(log.LstdFlags | log.Lshortfile)

	// ---------- configuration ----------
	listenAddr := envOr("LISTEN_ADDR", ":80")
	djangoBackend := envOr("DJANGO_BACKEND", "web:8000")
	locationService := envOr("LOCATION_SERVICE", "location:8001")
	matchingService := envOr("MATCHING_SERVICE", "matching:8002")
	whatsappService := envOr("WHATSAPP_SERVICE", "whatsapp:8004")
	staticDir := envOr("STATIC_DIR", "")
	secretKey = []byte(os.Getenv("SECRET_KEY"))

	// ---------- upstreams ----------
	djangoProxy := newReverseProxy(djangoBackend)
	locationProxy := websocketProxy(locationService)
	matchingProxy := newReverseProxy(matchingService)
	whatsappProxy := newReverseProxy(whatsappService)

	// ---------- rate limiters ----------
	generalRL := newRateLimiterGroup(60, 20)   // 60/min, burst 20
	authRL := newRateLimiterGroup(10, 5)       // 10/min, burst 5
	deliveryRL := newRateLimiterGroup(10, 3)   // 10/min, burst 3

	// ---------- router ----------
	mux := http.NewServeMux()

	// Health check (not rate-limited).
	mux.HandleFunc("/health", func(w http.ResponseWriter, r *http.Request) {
		w.Header().Set("Content-Type", "application/json")
		w.WriteHeader(http.StatusOK)
		_, _ = w.Write([]byte(`{"status":"ok"}`))
	})

	// WhatsApp webhook — GET verification handled here (fast), POST proxied to WhatsApp service.
	whatsappVerifyToken := envOr("WHATSAPP_VERIFY_TOKEN", "")
	mux.HandleFunc("/whatsapp/webhook", func(w http.ResponseWriter, r *http.Request) {
		if r.Method == http.MethodGet {
			// Meta verification challenge — echo back hub.challenge if token matches.
			mode := r.URL.Query().Get("hub.mode")
			token := r.URL.Query().Get("hub.verify_token")
			challenge := r.URL.Query().Get("hub.challenge")
			if mode == "subscribe" && token == whatsappVerifyToken {
				w.WriteHeader(http.StatusOK)
				_, _ = w.Write([]byte(challenge))
				log.Printf("whatsapp webhook verified")
				return
			}
			http.Error(w, "Forbidden", http.StatusForbidden)
			return
		}
		// POST — proxy to WhatsApp FastAPI service for message processing.
		whatsappProxy.ServeHTTP(w, r)
	})

	// WhatsApp internal API — blocked from external access.
	mux.Handle("/whatsapp/internal/", internalOnly(whatsappProxy))

	// Prometheus metrics endpoint — scraped by Prometheus every 15s.
	mux.Handle("/metrics", metricsHandler())

	// Static files.
	if staticDir != "" {
		mux.Handle("/static/", staticFileHandler(staticDir))
	}

	// WebSocket — location service (pass through, no rate limit).
	// Register both with and without trailing slash — Android client sends trailing slash.
	mux.Handle("/ws/driver_location", locationProxy)
	mux.Handle("/ws/driver_location/", locationProxy)

	// Internal matching endpoint — block external access.
	mux.Handle("/internal/match/", internalOnly(matchingProxy))

	// Auth endpoints — strict rate limit (versioned API).
	authHandler := authRL.limit(djangoProxy)
	mux.Handle("/api/v1/login/", authHandler)
	mux.Handle("/api/v1/signup/", authHandler)
	mux.Handle("/api/v1/driver/signup/", authHandler)
	mux.Handle("/api/v1/otp/send/", authHandler)
	mux.Handle("/api/v1/otp/verify/", authHandler)

	// Delivery request — medium rate limit (versioned API).
	mux.Handle("/api/v1/delivery/request/", deliveryRL.limit(djangoProxy))

	// Wrap the entire mux: for paths not matched by specific patterns above,
	// apply the general rate limiter to the Django proxy.
	rootHandler := &routingHandler{
		mux:          mux,
		defaultProxy: djangoProxy,
		generalRL:    generalRL,
	}

	// ---------- server ----------
	srv := &http.Server{
		Addr:              listenAddr,
		Handler:           instrumentHandler(requestLogger(maxBodySize(rootHandler))),
		ReadHeaderTimeout: 10 * time.Second,
		ReadTimeout:       30 * time.Second,
		IdleTimeout:       120 * time.Second,
		MaxHeaderBytes:    1 << 20, // 1 MB max header size
	}

	// Graceful shutdown.
	go func() {
		sigCh := make(chan os.Signal, 1)
		signal.Notify(sigCh, syscall.SIGINT, syscall.SIGTERM)
		<-sigCh
		log.Println("shutting down gateway...")
		shutCtx, shutCancel := context.WithTimeout(context.Background(), 15*time.Second)
		defer shutCancel()
		_ = srv.Shutdown(shutCtx)
	}()

	log.Printf("gateway listening on %s", listenAddr)
	log.Printf("  django → %s", djangoBackend)
	log.Printf("  location → %s", locationService)
	log.Printf("  matching → %s", matchingService)
	log.Printf("  whatsapp → %s", whatsappService)

	if err := srv.ListenAndServe(); err != nil && err != http.ErrServerClosed {
		log.Fatalf("server error: %v", err)
	}
	log.Println("gateway stopped")
}

// maxBodySize wraps a handler to limit request body size to 10 MB.
// WebSocket upgrades are excluded since they have their own framing.
// Also strips the X-WhatsApp-Internal header from external requests
// to prevent clients from spoofing internal WhatsApp service calls.
func maxBodySize(next http.Handler) http.Handler {
	const maxBytes = 10 << 20 // 10 MB
	return http.HandlerFunc(func(w http.ResponseWriter, r *http.Request) {
		// Strip internal header from external requests to prevent OTP bypass.
		r.Header.Del("X-WhatsApp-Internal")

		// Skip body limit for WebSocket upgrade requests.
		if strings.EqualFold(r.Header.Get("Upgrade"), "websocket") {
			next.ServeHTTP(w, r)
			return
		}
		r.Body = http.MaxBytesReader(w, r.Body, maxBytes)
		next.ServeHTTP(w, r)
	})
}

// routingHandler dispatches requests. Specific paths go through the mux with
// their own rate limiters; everything else goes to Django with the general limiter.
type routingHandler struct {
	mux          *http.ServeMux
	defaultProxy http.Handler
	generalRL    *rateLimiterGroup
}

func (rh *routingHandler) ServeHTTP(w http.ResponseWriter, r *http.Request) {
	path := r.URL.Path

	// Specific routes handled by mux patterns.
	switch {
	case path == "/health":
		rh.mux.ServeHTTP(w, r)
	case path == "/metrics":
		rh.mux.ServeHTTP(w, r)
	case strings.HasPrefix(path, "/static/"):
		rh.mux.ServeHTTP(w, r)
	case strings.HasPrefix(path, "/ws/driver_location"):
		rh.mux.ServeHTTP(w, r)
	case strings.HasPrefix(path, "/internal/match"):
		rh.mux.ServeHTTP(w, r)
	case strings.HasPrefix(path, "/api/v1/login/"):
		rh.mux.ServeHTTP(w, r)
	case strings.HasPrefix(path, "/api/v1/signup/"):
		rh.mux.ServeHTTP(w, r)
	case strings.HasPrefix(path, "/api/v1/driver/signup/"):
		rh.mux.ServeHTTP(w, r)
	case strings.HasPrefix(path, "/api/v1/otp/"):
		rh.mux.ServeHTTP(w, r)
	case strings.HasPrefix(path, "/api/v1/delivery/request/"):
		rh.mux.ServeHTTP(w, r)
	case strings.HasPrefix(path, "/whatsapp/"):
		rh.mux.ServeHTTP(w, r)
	default:
		// General rate-limited Django proxy.
		rh.generalRL.limit(rh.defaultProxy).ServeHTTP(w, r)
	}
}

// internalOnly blocks requests not from Docker/K8s internal networks.
func internalOnly(next http.Handler) http.Handler {
	// Private network CIDRs.
	_, docker, _ := net.ParseCIDR("172.16.0.0/12")
	_, k8s, _ := net.ParseCIDR("10.0.0.0/8")
	_, local, _ := net.ParseCIDR("192.168.0.0/16")

	return http.HandlerFunc(func(w http.ResponseWriter, r *http.Request) {
		ip := extractIP(r)
		parsed := net.ParseIP(ip)
		if parsed == nil || (!docker.Contains(parsed) && !k8s.Contains(parsed) && !local.Contains(parsed) && !parsed.IsLoopback()) {
			http.Error(w, `{"detail":"forbidden"}`, http.StatusForbidden)
			return
		}
		next.ServeHTTP(w, r)
	})
}

func envOr(key, fallback string) string {
	if v := os.Getenv(key); v != "" {
		return v
	}
	return fallback
}
