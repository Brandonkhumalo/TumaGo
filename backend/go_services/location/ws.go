package main

import (
	"context"
	"encoding/json"
	"log"
	"net"
	"net/http"
	"sync"
	"time"

	"github.com/gorilla/websocket"
	"github.com/redis/go-redis/v9"
	"golang.org/x/time/rate"
)

const driverGeoKey = "driver_locations"

// wsConnLimiter limits WebSocket connection attempts per IP.
// Allows 5 connection attempts per minute with a burst of 3.
var wsConnLimiter = &connRateLimiter{
	limiters: make(map[string]*wsIPEntry),
	rps:      rate.Limit(5.0 / 60.0), // 5 per minute
	burst:    3,
}

type wsIPEntry struct {
	limiter  *rate.Limiter
	lastSeen time.Time
}

type connRateLimiter struct {
	mu       sync.Mutex
	limiters map[string]*wsIPEntry
	rps      rate.Limit
	burst    int
}

func (cl *connRateLimiter) allow(ip string) bool {
	cl.mu.Lock()
	defer cl.mu.Unlock()

	entry, exists := cl.limiters[ip]
	if !exists {
		entry = &wsIPEntry{limiter: rate.NewLimiter(cl.rps, cl.burst)}
		cl.limiters[ip] = entry
	}
	entry.lastSeen = time.Now()
	return entry.limiter.Allow()
}

// cleanup removes stale IP entries every 5 minutes.
func (cl *connRateLimiter) cleanup() {
	for {
		time.Sleep(5 * time.Minute)
		cl.mu.Lock()
		for ip, entry := range cl.limiters {
			if time.Since(entry.lastSeen) > 10*time.Minute {
				delete(cl.limiters, ip)
			}
		}
		cl.mu.Unlock()
	}
}

func init() {
	go wsConnLimiter.cleanup()
}

var upgrader = websocket.Upgrader{
	ReadBufferSize:  256,
	WriteBufferSize: 256,
	// Allow all origins — the token is the auth gate.
	CheckOrigin: func(r *http.Request) bool { return true },
}

type gpsMessage struct {
	Latitude  float64 `json:"latitude"`
	Longitude float64 `json:"longitude"`
}

// handleDriverWS is the HTTP handler mounted at /ws/driver_location.
func handleDriverWS(rdb *redis.Client) http.HandlerFunc {
	return func(w http.ResponseWriter, r *http.Request) {
		// Rate limit WebSocket connection attempts per IP.
		ip := extractIP(r)
		if !wsConnLimiter.allow(ip) {
			http.Error(w, `{"detail":"rate limit exceeded"}`, http.StatusTooManyRequests)
			return
		}

		token := r.URL.Query().Get("token")
		driverID, err := validateToken(token)
		if err != nil {
			log.Printf("auth failed: %v", err)
			// Reject before upgrade — send 403.
			http.Error(w, "authentication failed", http.StatusForbidden)
			return
		}

		conn, err := upgrader.Upgrade(w, r, nil)
		if err != nil {
			log.Printf("ws upgrade error: %v", err)
			return
		}
		defer conn.Close()

		log.Printf("driver %s connected", driverID)

		// Ensure cleanup on disconnect.
		defer func() {
			ctx, cancel := context.WithTimeout(context.Background(), 3*time.Second)
			defer cancel()
			rdb.ZRem(ctx, driverGeoKey, driverID)
			log.Printf("driver %s disconnected", driverID)
		}()

		// Set sensible read limits and deadlines.
		conn.SetReadLimit(512)
		_ = conn.SetReadDeadline(time.Now().Add(60 * time.Second))
		conn.SetPongHandler(func(string) error {
			_ = conn.SetReadDeadline(time.Now().Add(60 * time.Second))
			return nil
		})

		// Start a goroutine to send periodic pings so idle connections
		// are detected promptly.
		done := make(chan struct{})
		defer close(done)
		go func() {
			ticker := time.NewTicker(30 * time.Second)
			defer ticker.Stop()
			for {
				select {
				case <-ticker.C:
					_ = conn.SetWriteDeadline(time.Now().Add(5 * time.Second))
					if err := conn.WriteMessage(websocket.PingMessage, nil); err != nil {
						return
					}
				case <-done:
					return
				}
			}
		}()

		for {
			_, raw, err := conn.ReadMessage()
			if err != nil {
				// Normal close or network error — both handled by defer.
				break
			}

			// Reset read deadline on every message.
			_ = conn.SetReadDeadline(time.Now().Add(60 * time.Second))

			var msg gpsMessage
			if err := json.Unmarshal(raw, &msg); err != nil {
				continue
			}

			// Basic sanity check.
			if msg.Latitude < -90 || msg.Latitude > 90 || msg.Longitude < -180 || msg.Longitude > 180 {
				continue
			}

			ctx := context.Background()

			// 1. Update Redis GEO set.
			rdb.GeoAdd(ctx, driverGeoKey, &redis.GeoLocation{
				Name:      driverID,
				Longitude: msg.Longitude,
				Latitude:  msg.Latitude,
			})

			// 2. Upsert DB row (runs synchronously in the goroutine —
			//    pgx pool handles concurrency).
			upsertDriverLocation(ctx, driverID, msg.Latitude, msg.Longitude)
		}
	}
}

// extractIP returns the client IP from the request, respecting proxy headers.
func extractIP(r *http.Request) string {
	if ip := r.Header.Get("X-Real-IP"); ip != "" {
		return ip
	}
	if xff := r.Header.Get("X-Forwarded-For"); xff != "" {
		for i, c := range xff {
			if c == ',' {
				return xff[:i]
			}
		}
		return xff
	}
	host, _, err := net.SplitHostPort(r.RemoteAddr)
	if err != nil {
		return r.RemoteAddr
	}
	return host
}
