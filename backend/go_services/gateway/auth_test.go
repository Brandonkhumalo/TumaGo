package main

import (
	"crypto/hmac"
	"crypto/sha256"
	"encoding/base64"
	"encoding/json"
	"testing"
	"time"
)

// helper: build a valid HS256 JWT for testing.
func makeTestJWT(t *testing.T, claims map[string]interface{}, secret []byte) string {
	t.Helper()
	header := base64.RawURLEncoding.EncodeToString([]byte(`{"alg":"HS256","typ":"JWT"}`))
	payload, _ := json.Marshal(claims)
	payloadB64 := base64.RawURLEncoding.EncodeToString(payload)
	signingInput := header + "." + payloadB64
	mac := hmac.New(sha256.New, secret)
	mac.Write([]byte(signingInput))
	sig := base64.RawURLEncoding.EncodeToString(mac.Sum(nil))
	return signingInput + "." + sig
}

func init() {
	secretKey = []byte("test-secret-key-for-unit-tests")
}

func TestValidateJWT_ValidToken(t *testing.T) {
	claims := map[string]interface{}{
		"id":   "user-uuid-123",
		"type": "access_token",
		"exp":  float64(time.Now().Add(time.Hour).Unix()),
	}
	token := makeTestJWT(t, claims, secretKey)

	userID, err := validateJWT(token)
	if err != nil {
		t.Fatalf("unexpected error: %v", err)
	}
	if userID != "user-uuid-123" {
		t.Errorf("expected user-uuid-123, got %s", userID)
	}
}

func TestValidateJWT_EmptyToken(t *testing.T) {
	_, err := validateJWT("")
	if err == nil || err.Error() != "empty token" {
		t.Errorf("expected 'empty token' error, got: %v", err)
	}
}

func TestValidateJWT_MalformedToken(t *testing.T) {
	_, err := validateJWT("not.a.valid.jwt.format")
	if err == nil {
		t.Error("expected error for malformed token")
	}
}

func TestValidateJWT_InvalidSignature(t *testing.T) {
	claims := map[string]interface{}{
		"id":   "user-uuid",
		"type": "access_token",
		"exp":  float64(time.Now().Add(time.Hour).Unix()),
	}
	token := makeTestJWT(t, claims, []byte("wrong-secret"))

	_, err := validateJWT(token)
	if err == nil || err.Error() != "invalid signature" {
		t.Errorf("expected 'invalid signature' error, got: %v", err)
	}
}

func TestValidateJWT_ExpiredToken(t *testing.T) {
	claims := map[string]interface{}{
		"id":   "user-uuid",
		"type": "access_token",
		"exp":  float64(time.Now().Add(-time.Hour).Unix()),
	}
	token := makeTestJWT(t, claims, secretKey)

	_, err := validateJWT(token)
	if err == nil || err.Error() != "token expired" {
		t.Errorf("expected 'token expired' error, got: %v", err)
	}
}

func TestValidateJWT_WrongTokenType(t *testing.T) {
	claims := map[string]interface{}{
		"id":   "user-uuid",
		"type": "refresh_token",
		"exp":  float64(time.Now().Add(time.Hour).Unix()),
	}
	token := makeTestJWT(t, claims, secretKey)

	_, err := validateJWT(token)
	if err == nil || err.Error() != "wrong token type" {
		t.Errorf("expected 'wrong token type' error, got: %v", err)
	}
}

func TestValidateJWT_MissingUserID(t *testing.T) {
	claims := map[string]interface{}{
		"type": "access_token",
		"exp":  float64(time.Now().Add(time.Hour).Unix()),
	}
	token := makeTestJWT(t, claims, secretKey)

	_, err := validateJWT(token)
	if err == nil || err.Error() != "token missing user id" {
		t.Errorf("expected 'token missing user id' error, got: %v", err)
	}
}

func TestValidateJWT_NoExpiration(t *testing.T) {
	claims := map[string]interface{}{
		"id":   "user-uuid",
		"type": "access_token",
	}
	token := makeTestJWT(t, claims, secretKey)

	_, err := validateJWT(token)
	if err == nil || err.Error() != "token has no expiration" {
		t.Errorf("expected 'token has no expiration' error, got: %v", err)
	}
}
