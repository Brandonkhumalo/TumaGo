package main

import (
	"log"
	"net/http"
	"net/http/httputil"
	"net/url"
	"strings"
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

// websocketProxy creates a reverse proxy that preserves WebSocket upgrade headers.
func websocketProxy(target string) http.Handler {
	u, err := url.Parse("http://" + target)
	if err != nil {
		log.Fatalf("invalid upstream URL %q: %v", target, err)
	}

	proxy := httputil.NewSingleHostReverseProxy(u)

	// Override the Director to forward WebSocket headers.
	defaultDirector := proxy.Director
	proxy.Director = func(req *http.Request) {
		defaultDirector(req)
		// Preserve hop-by-hop headers needed for WebSocket upgrade.
		if strings.EqualFold(req.Header.Get("Upgrade"), "websocket") {
			req.Header.Set("Connection", "Upgrade")
			req.Header.Set("Upgrade", "websocket")
		}
		req.Host = u.Host
	}

	proxy.ErrorHandler = func(w http.ResponseWriter, r *http.Request, err error) {
		log.Printf("ws proxy error [%s]: %v", target, err)
		http.Error(w, `{"detail":"upstream unavailable"}`, http.StatusBadGateway)
	}

	return proxy
}

// staticFileHandler serves static files if STATIC_DIR is set, otherwise 404.
func staticFileHandler(dir string) http.Handler {
	if dir == "" {
		return http.NotFoundHandler()
	}
	return http.StripPrefix("/static/", http.FileServer(http.Dir(dir)))
}
