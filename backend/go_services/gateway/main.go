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
	staticDir := envOr("STATIC_DIR", "")
	secretKey = []byte(os.Getenv("SECRET_KEY"))

	// ---------- upstreams ----------
	djangoProxy := newReverseProxy(djangoBackend)
	locationProxy := websocketProxy(locationService)
	matchingProxy := newReverseProxy(matchingService)

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

	// Auth endpoints — strict rate limit.
	authHandler := authRL.limit(djangoProxy)
	mux.Handle("/login/", authHandler)
	mux.Handle("/signup/", authHandler)
	mux.Handle("/driver/signup/", authHandler)

	// Delivery request — medium rate limit.
	mux.Handle("/delivery/request/", deliveryRL.limit(djangoProxy))

	// Everything else → Django with general rate limit.
	// Use a custom handler because ServeMux needs exact or wildcard patterns.
	handler := http.HandlerFunc(func(w http.ResponseWriter, r *http.Request) {
		// Check if the path was already handled by a more specific pattern.
		// ServeMux routes to "/" as a catch-all.
		mux.ServeHTTP(w, r)
	})

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
		Handler:           rootHandler,
		ReadHeaderTimeout: 10 * time.Second,
		IdleTimeout:       120 * time.Second,
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

	_ = handler // suppress unused warning
	if err := srv.ListenAndServe(); err != nil && err != http.ErrServerClosed {
		log.Fatalf("server error: %v", err)
	}
	log.Println("gateway stopped")
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
	case strings.HasPrefix(path, "/static/"):
		rh.mux.ServeHTTP(w, r)
	case strings.HasPrefix(path, "/ws/driver_location"):
		rh.mux.ServeHTTP(w, r)
	case strings.HasPrefix(path, "/internal/match"):
		rh.mux.ServeHTTP(w, r)
	case strings.HasPrefix(path, "/login/"):
		rh.mux.ServeHTTP(w, r)
	case strings.HasPrefix(path, "/signup/"):
		rh.mux.ServeHTTP(w, r)
	case strings.HasPrefix(path, "/driver/signup/"):
		rh.mux.ServeHTTP(w, r)
	case strings.HasPrefix(path, "/delivery/request/"):
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
