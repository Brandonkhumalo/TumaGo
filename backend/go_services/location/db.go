package main

import (
	"context"
	"log"
	"os"
	"strconv"
	"time"

	"github.com/jackc/pgx/v5/pgxpool"
)

var dbPool *pgxpool.Pool

func initDB() {
	dsn := os.Getenv("DATABASE_URL")
	if dsn == "" {
		dsn = "postgres://postgres:postgres@pgbouncer:5432/postgres?sslmode=disable"
	}

	cfg, err := pgxpool.ParseConfig(dsn)
	if err != nil {
		log.Fatalf("failed to parse DATABASE_URL: %v", err)
	}

	cfg.MinConns = 5
	cfg.MaxConns = 40
	cfg.MaxConnLifetime = 30 * time.Minute
	cfg.MaxConnIdleTime = 5 * time.Minute

	pool, err := pgxpool.NewWithConfig(context.Background(), cfg)
	if err != nil {
		log.Fatalf("failed to create db pool: %v", err)
	}

	// Verify connectivity.
	ctx, cancel := context.WithTimeout(context.Background(), 5*time.Second)
	defer cancel()
	if err := pool.Ping(ctx); err != nil {
		log.Fatalf("db ping failed: %v", err)
	}

	dbPool = pool
	log.Println("PostgreSQL pool initialised")
}

func closeDB() {
	if dbPool != nil {
		dbPool.Close()
	}
}

// upsertDriverLocation writes one row per driver into
// "TumaGo_Server_driverlocations" using ON CONFLICT on the unique driver_id
// column (enforced by Django OneToOneField).
func upsertDriverLocation(ctx context.Context, driverID string, lat, lng float64) {
	if dbPool == nil {
		return
	}

	latStr := strconv.FormatFloat(lat, 'f', -1, 64)
	lngStr := strconv.FormatFloat(lng, 'f', -1, 64)

	_, err := dbPool.Exec(ctx,
		`INSERT INTO "TumaGo_Server_driverlocations" (driver_id, latitude, longitude)
		 VALUES ($1, $2, $3)
		 ON CONFLICT (driver_id) DO UPDATE SET latitude = $2, longitude = $3`,
		driverID, latStr, lngStr,
	)
	if err != nil {
		log.Printf("db upsert error for driver %s: %v", driverID, err)
	}
}

// markDriverOffline sets driver_online=false and driver_available=false
// when their WebSocket connection drops (app killed, phone died, etc).
func markDriverOffline(ctx context.Context, driverID string) {
	if dbPool == nil {
		return
	}

	_, err := dbPool.Exec(ctx,
		`UPDATE "TumaGo_Server_customuser"
		 SET driver_online = FALSE, driver_available = FALSE
		 WHERE id = $1 AND role = 'driver'`,
		driverID,
	)
	if err != nil {
		log.Printf("failed to mark driver %s offline: %v", driverID, err)
	}
}
