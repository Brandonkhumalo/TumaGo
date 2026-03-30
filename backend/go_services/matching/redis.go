package main

import (
	"context"
	"log"

	"github.com/redis/go-redis/v9"
)

const (
	driverGeoKey  = "driver_locations"
	busyDriverKey = "busy_drivers" // Redis SET of driver IDs currently on a trip
)

type nearbyDriver struct {
	ID       string
	Dist     float64 // distance in km
	Lat      float64
	Lng      float64
}

// geoSearchDrivers finds drivers within radiusKM of the given coordinates,
// sorted by distance ascending.
func geoSearchDrivers(ctx context.Context, rdb *redis.Client, lat, lng, radiusKM float64) ([]nearbyDriver, error) {
	results, err := rdb.GeoSearchLocation(ctx, driverGeoKey, &redis.GeoSearchLocationQuery{
		GeoSearchQuery: redis.GeoSearchQuery{
			Longitude:  lng,
			Latitude:   lat,
			Radius:     radiusKM,
			RadiusUnit: "km",
			Sort:       "ASC",
		},
		WithCoord: true,
		WithDist:  true,
	}).Result()
	if err != nil {
		return nil, err
	}

	// Filter out busy drivers (currently on a trip) in one SMISMEMBER call.
	names := make([]string, len(results))
	for i, r := range results {
		names[i] = r.Name
	}

	var busyFlags []bool
	if len(names) > 0 {
		flags, err := rdb.SMIsMember(ctx, busyDriverKey, stringsToIfaces(names)...).Result()
		if err != nil {
			// If Redis fails here, skip the filter — DB will catch busy drivers anyway.
			log.Printf("SMISMEMBER error (skipping busy filter): %v", err)
			busyFlags = make([]bool, len(names))
		} else {
			busyFlags = flags
		}
	}

	drivers := make([]nearbyDriver, 0, len(results))
	for i, r := range results {
		if busyFlags[i] {
			continue // skip driver currently on a trip
		}
		drivers = append(drivers, nearbyDriver{
			ID:   r.Name,
			Dist: r.Dist,
			Lat:  r.Latitude,
			Lng:  r.Longitude,
		})
	}

	log.Printf("GEOSEARCH found %d drivers within %.0fkm (%d busy excluded)", len(drivers), radiusKM, len(results)-len(drivers))
	return drivers, nil
}

// stringsToIfaces converts a []string to []interface{} for Redis SMISMEMBER.
func stringsToIfaces(ss []string) []interface{} {
	out := make([]interface{}, len(ss))
	for i, s := range ss {
		out[i] = s
	}
	return out
}
