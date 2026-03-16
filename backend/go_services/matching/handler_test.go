package main

import (
	"bytes"
	"encoding/json"
	"net/http"
	"net/http/httptest"
	"testing"
)

func TestWriteJSON(t *testing.T) {
	w := httptest.NewRecorder()
	resp := errorResponse{Detail: "test error"}
	writeJSON(w, http.StatusBadRequest, resp)

	if w.Code != http.StatusBadRequest {
		t.Errorf("expected 400, got %d", w.Code)
	}
	if ct := w.Header().Get("Content-Type"); ct != "application/json" {
		t.Errorf("expected application/json, got %s", ct)
	}

	var decoded errorResponse
	if err := json.NewDecoder(w.Body).Decode(&decoded); err != nil {
		t.Fatalf("failed to decode response: %v", err)
	}
	if decoded.Detail != "test error" {
		t.Errorf("expected 'test error', got %s", decoded.Detail)
	}
}

func TestMatchHandler_WrongMethod(t *testing.T) {
	r := httptest.NewRequest("GET", "/match", nil)
	w := httptest.NewRecorder()
	matchHandler(w, r)

	if w.Code != http.StatusMethodNotAllowed {
		t.Errorf("expected 405, got %d", w.Code)
	}
	if allow := w.Header().Get("Allow"); allow != "POST" {
		t.Errorf("expected Allow: POST, got %s", allow)
	}
}

func TestMatchHandler_InvalidJSON(t *testing.T) {
	r := httptest.NewRequest("POST", "/match", bytes.NewBufferString("not json"))
	r.Header.Set("Content-Type", "application/json")
	w := httptest.NewRecorder()
	matchHandler(w, r)

	if w.Code != http.StatusBadRequest {
		t.Errorf("expected 400, got %d", w.Code)
	}
}

func TestMatchHandler_MissingCoordinates(t *testing.T) {
	body := matchRequest{
		OriginLat:   0,
		OriginLng:   0,
		VehicleType: "van",
		TripID:      "trip-1",
	}
	b, _ := json.Marshal(body)
	r := httptest.NewRequest("POST", "/match", bytes.NewBuffer(b))
	r.Header.Set("Content-Type", "application/json")
	w := httptest.NewRecorder()
	matchHandler(w, r)

	if w.Code != http.StatusBadRequest {
		t.Errorf("expected 400, got %d", w.Code)
	}
	var resp errorResponse
	json.NewDecoder(w.Body).Decode(&resp)
	if resp.Detail != "origin coordinates required" {
		t.Errorf("unexpected detail: %s", resp.Detail)
	}
}

func TestMatchHandler_MissingVehicleType(t *testing.T) {
	body := matchRequest{
		OriginLat: -26.2041,
		OriginLng: 28.0473,
		TripID:    "trip-1",
	}
	b, _ := json.Marshal(body)
	r := httptest.NewRequest("POST", "/match", bytes.NewBuffer(b))
	r.Header.Set("Content-Type", "application/json")
	w := httptest.NewRecorder()
	matchHandler(w, r)

	if w.Code != http.StatusBadRequest {
		t.Errorf("expected 400, got %d", w.Code)
	}
	var resp errorResponse
	json.NewDecoder(w.Body).Decode(&resp)
	if resp.Detail != "vehicle_type required" {
		t.Errorf("unexpected detail: %s", resp.Detail)
	}
}

func TestMatchRequest_JSONParsing(t *testing.T) {
	raw := `{"origin_lat": -26.2041, "origin_lng": 28.0473, "vehicle_type": "scooter", "trip_id": "abc-123"}`
	var req matchRequest
	if err := json.Unmarshal([]byte(raw), &req); err != nil {
		t.Fatalf("unmarshal error: %v", err)
	}
	if req.OriginLat != -26.2041 {
		t.Errorf("expected -26.2041, got %f", req.OriginLat)
	}
	if req.VehicleType != "scooter" {
		t.Errorf("expected scooter, got %s", req.VehicleType)
	}
	if req.TripID != "abc-123" {
		t.Errorf("expected abc-123, got %s", req.TripID)
	}
}

func TestMatchResponse_JSONSerialization(t *testing.T) {
	resp := matchResponse{
		DriverID:       "d-123",
		DriverName:     "John Doe",
		FCMToken:       "fcm-token",
		DriverLat:      -26.1,
		DriverLng:      28.0,
		DistanceMeters: 1500.5,
	}
	b, err := json.Marshal(resp)
	if err != nil {
		t.Fatalf("marshal error: %v", err)
	}

	var decoded map[string]interface{}
	json.Unmarshal(b, &decoded)
	if decoded["driver_id"] != "d-123" {
		t.Errorf("expected d-123, got %v", decoded["driver_id"])
	}
	if decoded["distance_meters"] != 1500.5 {
		t.Errorf("expected 1500.5, got %v", decoded["distance_meters"])
	}
}

func TestNearbyDriver_Struct(t *testing.T) {
	d := nearbyDriver{
		ID:   "driver-1",
		Dist: 2.5,
		Lat:  -26.2,
		Lng:  28.0,
	}
	if d.Dist*1000 != 2500 {
		t.Errorf("expected 2500m, got %f", d.Dist*1000)
	}
}

func TestDBDriver_Struct(t *testing.T) {
	d := dbDriver{
		ID:       "d-id",
		Name:     "Jane",
		Surname:  "Doe",
		FCMToken: "",
	}
	fullName := d.Name + " " + d.Surname
	if fullName != "Jane Doe" {
		t.Errorf("expected 'Jane Doe', got %s", fullName)
	}
}
