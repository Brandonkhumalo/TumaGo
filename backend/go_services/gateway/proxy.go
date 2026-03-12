package main

import (
	"io"
	"log"
	"net"
	"net/http"
	"net/http/httputil"
	"net/url"
	"time"
)

func newReverseProxy(target string) *httputil.ReverseProxy {
	u, err := url.Parse("http://" + target)
	if err != nil {
		log.Fatalf("invalid upstream URL %q: %v", target, err)
	}
	proxy := httputil.NewSingleHostReverseProxy(u)
	proxy.ErrorHandler = func(w http.ResponseWriter, r *http.Request, err error) {
		log.Printf("proxy error [%s]: %v", target, err)
		http.Error(w, `{"detail":"upstream unavailable"}`, http.StatusBadGateway)
	}
	return proxy
}

// websocketProxy creates a handler that tunnels WebSocket connections to the upstream.
// It hijacks the client connection and dials the upstream, then pipes bytes in both
// directions. This is necessary because httputil.ReverseProxy cannot handle WebSocket
// upgrades (it strips hop-by-hop headers like Connection and Upgrade).
func websocketProxy(target string) http.Handler {
	return http.HandlerFunc(func(w http.ResponseWriter, r *http.Request) {
		// Dial the upstream location service.
		upstreamConn, err := net.DialTimeout("tcp", target, 5*time.Second)
		if err != nil {
			log.Printf("ws proxy: dial %s failed: %v", target, err)
			http.Error(w, `{"detail":"upstream unavailable"}`, http.StatusBadGateway)
			return
		}

		// Hijack the client connection.
		hj, ok := w.(http.Hijacker)
		if !ok {
			log.Printf("ws proxy: hijack not supported")
			upstreamConn.Close()
			http.Error(w, `{"detail":"server error"}`, http.StatusInternalServerError)
			return
		}
		clientConn, clientBuf, err := hj.Hijack()
		if err != nil {
			log.Printf("ws proxy: hijack error: %v", err)
			upstreamConn.Close()
			return
		}

		// Forward the original HTTP request (including Upgrade headers) to upstream.
		if err := r.Write(upstreamConn); err != nil {
			log.Printf("ws proxy: write request to upstream failed: %v", err)
			clientConn.Close()
			upstreamConn.Close()
			return
		}

		log.Printf("ws proxy: tunneling %s → %s", r.RemoteAddr, target)

		// Pipe data in both directions.
		done := make(chan struct{}, 2)
		go func() {
			// upstream → client
			io.Copy(clientConn, upstreamConn)
			done <- struct{}{}
		}()
		go func() {
			// client → upstream (flush any buffered data first)
			if clientBuf.Reader.Buffered() > 0 {
				buffered := make([]byte, clientBuf.Reader.Buffered())
				clientBuf.Read(buffered)
				upstreamConn.Write(buffered)
			}
			io.Copy(upstreamConn, clientConn)
			done <- struct{}{}
		}()

		// Wait for one direction to finish, then close both.
		<-done
		clientConn.Close()
		upstreamConn.Close()
		<-done
	})
}

// staticFileHandler serves static files if STATIC_DIR is set, otherwise 404.
func staticFileHandler(dir string) http.Handler {
	if dir == "" {
		return http.NotFoundHandler()
	}
	return http.StripPrefix("/static/", http.FileServer(http.Dir(dir)))
}
