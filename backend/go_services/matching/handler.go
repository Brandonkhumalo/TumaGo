package main

import (
	"encoding/json"
	"log"
	"net/http"
	"strings"
)

type matchRequest struct {
	OriginLat     float64 `json:"origin_lat"`
	OriginLng     float64 `json:"origin_lng"`
	VehicleType   string  `json:"vehicle_type"`
	TripID        string  `json:"trip_id"`
	Fare          float64 `json:"fare"`
	PaymentMethod string  `json:"payment_method"`
}

type matchResponse struct {
	DriverID      string  `json:"driver_id"`
	DriverName    string  `json:"driver_name"`
	FCMToken      string  `json:"fcm_token"`
	DriverLat     float64 `json:"driver_lat"`
	DriverLng     float64 `json:"driver_lng"`
	DistanceMeters float64 `json:"distance_meters"`
}

type errorResponse struct {
	Detail string `json:"detail"`
}

func matchHandler(w http.ResponseWriter, r *http.Request) {
	if r.Method != http.MethodPost {
		w.Header().Set("Allow", "POST")
		http.Error(w, `{"detail":"method not allowed"}`, http.StatusMethodNotAllowed)
		return
	}

	// Limit request body to 1KB — match requests are small JSON payloads.
	r.Body = http.MaxBytesReader(w, r.Body, 1024)

	var req matchRequest
	if err := json.NewDecoder(r.Body).Decode(&req); err != nil {
		writeJSON(w, http.StatusBadRequest, errorResponse{Detail: "invalid request body"})
		return
	}

	if req.OriginLat == 0 && req.OriginLng == 0 {
		writeJSON(w, http.StatusBadRequest, errorResponse{Detail: "origin coordinates required"})
		return
	}
	if req.VehicleType == "" {
		writeJSON(w, http.StatusBadRequest, errorResponse{Detail: "vehicle_type required"})
		return
	}

	ctx := r.Context()

	// 1. Redis GEOSEARCH — find nearby drivers sorted by distance.
	nearby, err := geoSearchDrivers(ctx, rdb, req.OriginLat, req.OriginLng, searchRadiusKM)
	if err != nil {
		log.Printf("GEOSEARCH error: %v", err)
		writeJSON(w, http.StatusInternalServerError, errorResponse{Detail: "search failed"})
		return
	}
	if len(nearby) == 0 {
		writeJSON(w, http.StatusNotFound, errorResponse{Detail: "no drivers in radius"})
		return
	}

	// 2. Collect IDs in distance order.
	driverIDs := make([]string, len(nearby))
	coordsMap := make(map[string]nearbyDriver, len(nearby))
	for i, d := range nearby {
		driverIDs[i] = d.ID
		coordsMap[d.ID] = d
	}

	// Calculate minimum wallet balance (commission) the driver must have.
	// 15% for cash, 20% for online payments.
	commissionRate := 0.15
	pm := strings.ToLower(req.PaymentMethod)
	if pm == "card" || pm == "ecocash" || pm == "onemoney" {
		commissionRate = 0.20
	}
	minWalletBalance := req.Fare * commissionRate

	// 3. Single DB query — filter by availability + vehicle type + wallet balance.
	dbDrivers, err := findAvailableDrivers(ctx, driverIDs, req.VehicleType, minWalletBalance)
	if err != nil {
		log.Printf("DB query error: %v", err)
		writeJSON(w, http.StatusInternalServerError, errorResponse{Detail: "database error"})
		return
	}

	// 4. Return the closest driver that passed DB filters.
	for _, id := range driverIDs {
		d, ok := dbDrivers[id]
		if !ok {
			continue
		}
		coords := coordsMap[id]
		resp := matchResponse{
			DriverID:       d.ID,
			DriverName:     d.Name + " " + d.Surname,
			FCMToken:       d.FCMToken,
			DriverLat:      coords.Lat,
			DriverLng:      coords.Lng,
			DistanceMeters: coords.Dist * 1000, // km → meters
		}
		log.Printf("matched driver %s (%s) at %.0fm for trip %s",
			d.ID, resp.DriverName, resp.DistanceMeters, req.TripID)
		writeJSON(w, http.StatusOK, resp)
		return
	}

	writeJSON(w, http.StatusNotFound, errorResponse{Detail: "no available drivers with that vehicle type"})
}

func writeJSON(w http.ResponseWriter, status int, v interface{}) {
	w.Header().Set("Content-Type", "application/json")
	w.WriteHeader(status)
	_ = json.NewEncoder(w).Encode(v)
}
