package main

import (
	"github.com/prometheus/client_golang/prometheus"
	"github.com/prometheus/client_golang/prometheus/promhttp"
	"net/http"
)

// Prometheus metrics for the location service.
var (
	// wsConnectionsActive tracks the number of active WebSocket connections.
	wsConnectionsActive = prometheus.NewGauge(
		prometheus.GaugeOpts{
			Name: "location_ws_connections_active",
			Help: "Number of active WebSocket connections from drivers.",
		},
	)

	// wsConnectionsTotal counts total WebSocket connections since startup.
	wsConnectionsTotal = prometheus.NewCounter(
		prometheus.CounterOpts{
			Name: "location_ws_connections_total",
			Help: "Total WebSocket connections accepted.",
		},
	)

	// wsMessagesReceived counts GPS messages received from drivers.
	wsMessagesReceived = prometheus.NewCounter(
		prometheus.CounterOpts{
			Name: "location_ws_messages_received_total",
			Help: "Total GPS messages received via WebSocket.",
		},
	)

	// wsAuthFailures counts authentication failures during WebSocket handshake.
	wsAuthFailures = prometheus.NewCounter(
		prometheus.CounterOpts{
			Name: "location_ws_auth_failures_total",
			Help: "Total WebSocket authentication failures.",
		},
	)

	// redisGeoUpdates counts successful Redis GEOADD operations.
	redisGeoUpdates = prometheus.NewCounter(
		prometheus.CounterOpts{
			Name: "location_redis_geo_updates_total",
			Help: "Total Redis GEOADD operations for driver locations.",
		},
	)

	// dbUpserts counts successful DB upsert operations.
	dbUpserts = prometheus.NewCounter(
		prometheus.CounterOpts{
			Name: "location_db_upserts_total",
			Help: "Total PostgreSQL upsert operations for driver locations.",
		},
	)
)

func init() {
	prometheus.MustRegister(
		wsConnectionsActive,
		wsConnectionsTotal,
		wsMessagesReceived,
		wsAuthFailures,
		redisGeoUpdates,
		dbUpserts,
	)
}

// metricsHandler returns the Prometheus HTTP handler for /metrics.
func metricsHandler() http.Handler {
	return promhttp.Handler()
}
