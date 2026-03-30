package main

import (
	"context"
	"log"
	"os"
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
	cfg.MaxConns = 30
	cfg.MaxConnLifetime = 30 * time.Minute
	cfg.MaxConnIdleTime = 5 * time.Minute

	pool, err := pgxpool.NewWithConfig(context.Background(), cfg)
	if err != nil {
		log.Fatalf("failed to create db pool: %v", err)
	}

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

type dbDriver struct {
	ID       string
	Name     string
	Surname  string
	FCMToken string
}

// findAvailableDrivers queries the DB for available drivers with the matching
// vehicle type and sufficient wallet balance from the given list of IDs.
func findAvailableDrivers(ctx context.Context, driverIDs []string, vehicleType string, minWalletBalance float64) (map[string]dbDriver, error) {
	if dbPool == nil || len(driverIDs) == 0 {
		return nil, nil
	}

	rows, err := dbPool.Query(ctx,
		`SELECT u.id::text, u.name, u.surname, COALESCE(u.fcm_token, '')
		 FROM "TumaGo_Server_customuser" u
		 JOIN "TumaGo_Server_drivervehicle" v ON v.driver_id = u.id
		 LEFT JOIN "TumaGo_Server_driverwallet" w ON w.driver_id = u.id
		 WHERE u.id::text = ANY($1)
		   AND u.driver_available = TRUE
		   AND u.role = 'driver'
		   AND v.delivery_vehicle = $2
		   AND COALESCE(w.balance, 0) >= $3`,
		driverIDs, vehicleType, minWalletBalance,
	)
	if err != nil {
		return nil, err
	}
	defer rows.Close()

	result := make(map[string]dbDriver)
	for rows.Next() {
		var d dbDriver
		if err := rows.Scan(&d.ID, &d.Name, &d.Surname, &d.FCMToken); err != nil {
			return nil, err
		}
		result[d.ID] = d
	}

	return result, rows.Err()
}
