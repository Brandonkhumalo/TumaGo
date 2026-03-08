package main

import (
	"context"
	"log"
	"net/http"
	"os"
	"os/signal"
	"strconv"
	"syscall"
	"time"

	"github.com/redis/go-redis/v9"
)

var (
	rdb            *redis.Client
	searchRadiusKM float64
)

func main() {
	log.SetFlags(log.LstdFlags | log.Lshortfile)

	// ---------- configuration ----------
	listenAddr := envOr("LISTEN_ADDR", ":8002")
	redisURL := envOr("REDIS_URL", "redis://redis:6379")
	searchRadiusKM = 15.0
	if v := os.Getenv("SEARCH_RADIUS_KM"); v != "" {
		if parsed, err := strconv.ParseFloat(v, 64); err == nil {
			searchRadiusKM = parsed
		}
	}

	// ---------- init PostgreSQL ----------
	initDB()
	defer closeDB()

	// ---------- init Redis ----------
	opts, err := redis.ParseURL(redisURL)
	if err != nil {
		log.Fatalf("invalid REDIS_URL: %v", err)
	}
	opts.PoolSize = 50
	opts.MinIdleConns = 5
	rdb = redis.NewClient(opts)

	ctx, cancel := context.WithTimeout(context.Background(), 5*time.Second)
	defer cancel()
	if err := rdb.Ping(ctx).Err(); err != nil {
		log.Fatalf("redis ping failed: %v", err)
	}
	log.Println("Redis connected")

	// ---------- HTTP routes ----------
	mux := http.NewServeMux()
	mux.HandleFunc("/health", func(w http.ResponseWriter, r *http.Request) {
		w.Header().Set("Content-Type", "application/json")
		w.WriteHeader(http.StatusOK)
		_, _ = w.Write([]byte(`{"status":"ok"}`))
	})
	mux.HandleFunc("/match", matchHandler)

	// ---------- server ----------
	srv := &http.Server{
		Addr:              listenAddr,
		Handler:           mux,
		ReadHeaderTimeout: 5 * time.Second,
		IdleTimeout:       60 * time.Second,
	}

	go func() {
		sigCh := make(chan os.Signal, 1)
		signal.Notify(sigCh, syscall.SIGINT, syscall.SIGTERM)
		<-sigCh
		log.Println("shutting down matching service...")
		shutCtx, shutCancel := context.WithTimeout(context.Background(), 10*time.Second)
		defer shutCancel()
		_ = srv.Shutdown(shutCtx)
		_ = rdb.Close()
	}()

	log.Printf("matching service listening on %s (radius=%.0fkm)", listenAddr, searchRadiusKM)
	if err := srv.ListenAndServe(); err != nil && err != http.ErrServerClosed {
		log.Fatalf("server error: %v", err)
	}
	log.Println("matching service stopped")
}

func envOr(key, fallback string) string {
	if v := os.Getenv(key); v != "" {
		return v
	}
	return fallback
}
