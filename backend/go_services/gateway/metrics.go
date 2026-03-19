package main

import (
	"net/http"
	"strconv"
	"strings"
	"time"

	"github.com/prometheus/client_golang/prometheus"
	"github.com/prometheus/client_golang/prometheus/promhttp"
)

// Prometheus metrics for the API gateway.
var (
	// httpRequestsTotal counts all HTTP requests by method, path pattern, and status code.
	httpRequestsTotal = prometheus.NewCounterVec(
		prometheus.CounterOpts{
			Name: "gateway_http_requests_total",
			Help: "Total HTTP requests processed by the gateway.",
		},
		[]string{"method", "route", "status"},
	)

	// httpRequestDuration tracks request latency in seconds.
	httpRequestDuration = prometheus.NewHistogramVec(
		prometheus.HistogramOpts{
			Name:    "gateway_http_request_duration_seconds",
			Help:    "HTTP request duration in seconds.",
			Buckets: prometheus.DefBuckets,
		},
		[]string{"method", "route"},
	)

	// httpInFlight tracks the number of requests currently being served.
	httpInFlight = prometheus.NewGauge(
		prometheus.GaugeOpts{
			Name: "gateway_http_in_flight_requests",
			Help: "Number of HTTP requests currently being processed.",
		},
	)
)

func init() {
	prometheus.MustRegister(httpRequestsTotal, httpRequestDuration, httpInFlight)
}

// metricsHandler returns the Prometheus HTTP handler for /metrics.
func metricsHandler() http.Handler {
	return promhttp.Handler()
}

// instrumentHandler wraps an http.Handler to record Prometheus metrics.
// It captures status code, request count, duration, and in-flight gauge.
// WebSocket upgrades bypass the status wrapper to preserve http.Hijacker.
func instrumentHandler(next http.Handler) http.Handler {
	return http.HandlerFunc(func(w http.ResponseWriter, r *http.Request) {
		// Don't instrument the metrics endpoint itself.
		if r.URL.Path == "/metrics" {
			next.ServeHTTP(w, r)
			return
		}

		route := normalizeRoute(r.URL.Path)
		start := time.Now()

		httpInFlight.Inc()
		defer httpInFlight.Dec()

		// For WebSocket upgrades, pass the original ResponseWriter so Hijack works.
		if strings.EqualFold(r.Header.Get("Upgrade"), "websocket") {
			next.ServeHTTP(w, r)
			duration := time.Since(start).Seconds()
			httpRequestsTotal.WithLabelValues(r.Method, route, "101").Inc()
			httpRequestDuration.WithLabelValues(r.Method, route).Observe(duration)
			return
		}

		sw := &metricsStatusWriter{ResponseWriter: w, code: http.StatusOK}
		next.ServeHTTP(sw, r)

		duration := time.Since(start).Seconds()
		status := strconv.Itoa(sw.code)

		httpRequestsTotal.WithLabelValues(r.Method, route, status).Inc()
		httpRequestDuration.WithLabelValues(r.Method, route).Observe(duration)
	})
}

// metricsStatusWriter captures the HTTP status code for metrics recording.
type metricsStatusWriter struct {
	http.ResponseWriter
	code    int
	written bool
}

func (w *metricsStatusWriter) WriteHeader(code int) {
	if !w.written {
		w.code = code
		w.written = true
	}
	w.ResponseWriter.WriteHeader(code)
}

// normalizeRoute maps request paths to known route patterns so that
// high-cardinality paths (like /api/v1/delivery/123) don't explode
// the metric label space.
func normalizeRoute(path string) string {
	switch {
	case path == "/health":
		return "/health"
	case path == "/metrics":
		return "/metrics"
	case len(path) >= 8 && path[:8] == "/static/":
		return "/static/*"
	case len(path) >= 20 && path[:20] == "/ws/driver_location":
		return "/ws/driver_location"
	case len(path) >= 15 && path[:15] == "/internal/match":
		return "/internal/match"
	case len(path) >= 14 && path[:14] == "/api/v1/login/":
		return "/api/v1/login"
	case len(path) >= 15 && path[:15] == "/api/v1/signup/":
		return "/api/v1/signup"
	case len(path) >= 22 && path[:22] == "/api/v1/driver/signup/":
		return "/api/v1/driver/signup"
	case len(path) >= 25 && path[:25] == "/api/v1/delivery/request/":
		return "/api/v1/delivery/request"
	default:
		return "/api/v1/*"
	}
}
