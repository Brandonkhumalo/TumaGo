package main

import (
	"bufio"
	"fmt"
	"log"
	"net"
	"net/http"
	"strings"
	"time"
)

// statusWriter wraps http.ResponseWriter to capture the status code.
type statusWriter struct {
	http.ResponseWriter
	code int
}

func (sw *statusWriter) WriteHeader(code int) {
	sw.code = code
	sw.ResponseWriter.WriteHeader(code)
}

// Hijack implements http.Hijacker so WebSocket upgrades work through the logger.
func (sw *statusWriter) Hijack() (net.Conn, *bufio.ReadWriter, error) {
	if hj, ok := sw.ResponseWriter.(http.Hijacker); ok {
		return hj.Hijack()
	}
	return nil, nil, fmt.Errorf("upstream ResponseWriter does not support hijacking")
}

// requestLogger logs method, path, status code, and duration for every request.
// WebSocket upgrades are logged but bypass the status wrapper since the
// connection gets hijacked.
func requestLogger(next http.Handler) http.Handler {
	return http.HandlerFunc(func(w http.ResponseWriter, r *http.Request) {
		start := time.Now()
		// For WebSocket upgrades, pass the original ResponseWriter so Hijack works,
		// and log after the handler returns.
		if strings.EqualFold(r.Header.Get("Upgrade"), "websocket") {
			next.ServeHTTP(w, r)
			log.Printf("%s %s (ws) %s", r.Method, r.URL.Path, time.Since(start).Round(time.Millisecond))
			return
		}
		sw := &statusWriter{ResponseWriter: w, code: http.StatusOK}
		next.ServeHTTP(sw, r)
		log.Printf("%s %s %d %s", r.Method, r.URL.Path, sw.code, time.Since(start).Round(time.Millisecond))
	})
}
