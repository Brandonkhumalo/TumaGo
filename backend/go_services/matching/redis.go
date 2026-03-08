package main

import (
	"context"
	"log"

	"github.com/redis/go-redis/v9"
)

const driverGeoKey = "driver_locations"

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

	drivers := make([]nearbyDriver, 0, len(results))
	for _, r := range results {
		drivers = append(drivers, nearbyDriver{
			ID:   r.Name,
			Dist: r.Dist,
			Lat:  r.Latitude,
			Lng:  r.Longitude,
		})
	}

	log.Printf("GEOSEARCH found %d drivers within %.0fkm", len(drivers), radiusKM)
	return drivers, nil
}
