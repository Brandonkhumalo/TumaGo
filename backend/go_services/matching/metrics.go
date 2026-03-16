package main

import (
	"net/http"
	"strconv"
	"time"

	"github.com/prometheus/client_golang/prometheus"
	"github.com/prometheus/client_golang/prometheus/promhttp"
)

// Prometheus metrics for the matching service.
var (
	// matchRequestsTotal counts match requests by status code.
	matchRequestsTotal = prometheus.NewCounterVec(
		prometheus.CounterOpts{
			Name: "matching_requests_total",
			Help: "Total match requests by HTTP status code.",
		},
		[]string{"status"},
	)

	// matchRequestDuration tracks how long match requests take.
	matchRequestDuration = prometheus.NewHistogram(
		prometheus.HistogramOpts{
			Name:    "matching_request_duration_seconds",
			Help:    "Match request duration in seconds.",
			Buckets: prometheus.DefBuckets,
		},
	)

	// matchDriversFound tracks how many nearby drivers Redis returns per request.
	matchDriversFound = prometheus.NewHistogram(
		prometheus.HistogramOpts{
			Name:    "matching_drivers_found",
			Help:    "Number of nearby drivers found by GEOSEARCH per match request.",
			Buckets: []float64{0, 1, 2, 5, 10, 20, 50},
		},
	)

	// matchSuccessTotal counts successful matches (driver found).
	matchSuccessTotal = prometheus.NewCounter(
		prometheus.CounterOpts{
			Name: "matching_success_total",
			Help: "Total successful driver matches.",
		},
	)

	// matchNoDriverTotal counts requests where no driver was found.
	matchNoDriverTotal = prometheus.NewCounter(
		prometheus.CounterOpts{
			Name: "matching_no_driver_total",
			Help: "Total match requests where no available driver was found.",
		},
	)
)

func init() {
	prometheus.MustRegister(
		matchRequestsTotal,
		matchRequestDuration,
		matchDriversFound,
		matchSuccessTotal,
		matchNoDriverTotal,
	)
}

// metricsHandler returns the Prometheus HTTP handler for /metrics.
func metricsHandler() http.Handler {
	return promhttp.Handler()
}

// instrumentedMatchHandler wraps matchHandler with Prometheus metrics.
func instrumentedMatchHandler(w http.ResponseWriter, r *http.Request) {
	start := time.Now()
	sw := &matchStatusWriter{ResponseWriter: w, code: http.StatusOK}

	matchHandler(sw, r)

	duration := time.Since(start).Seconds()
	status := strconv.Itoa(sw.code)

	matchRequestsTotal.WithLabelValues(status).Inc()
	matchRequestDuration.Observe(duration)
}

// matchStatusWriter captures the HTTP status code for metrics.
type matchStatusWriter struct {
	http.ResponseWriter
	code    int
	written bool
}

func (w *matchStatusWriter) WriteHeader(code int) {
	if !w.written {
		w.code = code
		w.written = true
	}
	w.ResponseWriter.WriteHeader(code)
}
