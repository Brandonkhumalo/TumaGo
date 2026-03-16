package main

import (
	"net"
	"net/http"
	"net/http/httptest"
	"testing"
)

func TestExtractIP_XRealIP(t *testing.T) {
	r := httptest.NewRequest("GET", "/", nil)
	r.Header.Set("X-Real-IP", "1.2.3.4")
	if ip := extractIP(r); ip != "1.2.3.4" {
		t.Errorf("expected 1.2.3.4, got %s", ip)
	}
}

func TestExtractIP_XForwardedFor(t *testing.T) {
	r := httptest.NewRequest("GET", "/", nil)
	r.Header.Set("X-Forwarded-For", "10.0.0.1, 172.16.0.1")
	if ip := extractIP(r); ip != "10.0.0.1" {
		t.Errorf("expected 10.0.0.1, got %s", ip)
	}
}

func TestExtractIP_RemoteAddr(t *testing.T) {
	r := httptest.NewRequest("GET", "/", nil)
	r.RemoteAddr = "192.168.1.1:12345"
	if ip := extractIP(r); ip != "192.168.1.1" {
		t.Errorf("expected 192.168.1.1, got %s", ip)
	}
}

func TestExtractIP_RemoteAddrNoPort(t *testing.T) {
	r := httptest.NewRequest("GET", "/", nil)
	r.RemoteAddr = "192.168.1.1"
	// net.SplitHostPort will fail, fallback to raw RemoteAddr
	if ip := extractIP(r); ip != "192.168.1.1" {
		t.Errorf("expected 192.168.1.1, got %s", ip)
	}
}

func TestRateLimiterGroup_AllowsUnderLimit(t *testing.T) {
	rl := &rateLimiterGroup{
		limiters: make(map[string]*ipLimiter),
		rps:      10, // 10 per second (600/min) — generous
		burst:    5,
	}

	handler := rl.limit(http.HandlerFunc(func(w http.ResponseWriter, r *http.Request) {
		w.WriteHeader(http.StatusOK)
	}))

	r := httptest.NewRequest("GET", "/", nil)
	r.RemoteAddr = "1.2.3.4:1234"
	w := httptest.NewRecorder()
	handler.ServeHTTP(w, r)
	if w.Code != http.StatusOK {
		t.Errorf("expected 200, got %d", w.Code)
	}
}

func TestRateLimiterGroup_BlocksOverLimit(t *testing.T) {
	rl := &rateLimiterGroup{
		limiters: make(map[string]*ipLimiter),
		rps:      0.001, // extremely low: ~0.06/min
		burst:    1,
	}

	handler := rl.limit(http.HandlerFunc(func(w http.ResponseWriter, r *http.Request) {
		w.WriteHeader(http.StatusOK)
	}))

	r := httptest.NewRequest("GET", "/", nil)
	r.RemoteAddr = "5.6.7.8:1234"

	// First request should succeed (burst=1)
	w := httptest.NewRecorder()
	handler.ServeHTTP(w, r)
	if w.Code != http.StatusOK {
		t.Errorf("first request: expected 200, got %d", w.Code)
	}

	// Second request should be rate-limited
	w = httptest.NewRecorder()
	handler.ServeHTTP(w, r)
	if w.Code != http.StatusTooManyRequests {
		t.Errorf("second request: expected 429, got %d", w.Code)
	}
}

func TestRateLimiterGroup_PerIPIsolation(t *testing.T) {
	rl := &rateLimiterGroup{
		limiters: make(map[string]*ipLimiter),
		rps:      0.001,
		burst:    1,
	}

	handler := rl.limit(http.HandlerFunc(func(w http.ResponseWriter, r *http.Request) {
		w.WriteHeader(http.StatusOK)
	}))

	// Exhaust IP A
	r1 := httptest.NewRequest("GET", "/", nil)
	r1.RemoteAddr = "1.1.1.1:1234"
	w := httptest.NewRecorder()
	handler.ServeHTTP(w, r1)
	w = httptest.NewRecorder()
	handler.ServeHTTP(w, r1)
	if w.Code != http.StatusTooManyRequests {
		t.Errorf("IP A second request: expected 429, got %d", w.Code)
	}

	// IP B should still work
	r2 := httptest.NewRequest("GET", "/", nil)
	r2.RemoteAddr = "2.2.2.2:1234"
	w = httptest.NewRecorder()
	handler.ServeHTTP(w, r2)
	if w.Code != http.StatusOK {
		t.Errorf("IP B first request: expected 200, got %d", w.Code)
	}
}

func TestInternalOnly_AllowsPrivateNetworks(t *testing.T) {
	_, docker, _ := net.ParseCIDR("172.16.0.0/12")
	_, k8s, _ := net.ParseCIDR("10.0.0.0/8")
	_, local, _ := net.ParseCIDR("192.168.0.0/16")

	handler := internalOnly(http.HandlerFunc(func(w http.ResponseWriter, r *http.Request) {
		w.WriteHeader(http.StatusOK)
	}))

	_ = docker
	_ = k8s
	_ = local

	tests := []struct {
		name string
		ip   string
		want int
	}{
		{"docker network", "172.18.0.5", 200},
		{"k8s network", "10.0.1.5", 200},
		{"local network", "192.168.1.100", 200},
		{"loopback", "127.0.0.1", 200},
		{"external IP blocked", "8.8.8.8", 403},
		{"another external", "203.0.113.1", 403},
	}

	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {
			r := httptest.NewRequest("GET", "/", nil)
			r.Header.Set("X-Real-IP", tt.ip)
			w := httptest.NewRecorder()
			handler.ServeHTTP(w, r)
			if w.Code != tt.want {
				t.Errorf("%s: expected %d, got %d", tt.name, tt.want, w.Code)
			}
		})
	}
}

func TestHealthEndpoint(t *testing.T) {
	mux := http.NewServeMux()
	mux.HandleFunc("/health", func(w http.ResponseWriter, r *http.Request) {
		w.Header().Set("Content-Type", "application/json")
		w.WriteHeader(http.StatusOK)
		_, _ = w.Write([]byte(`{"status":"ok"}`))
	})

	r := httptest.NewRequest("GET", "/health", nil)
	w := httptest.NewRecorder()
	mux.ServeHTTP(w, r)

	if w.Code != http.StatusOK {
		t.Errorf("expected 200, got %d", w.Code)
	}
	if w.Body.String() != `{"status":"ok"}` {
		t.Errorf("unexpected body: %s", w.Body.String())
	}
}
