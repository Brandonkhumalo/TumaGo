package main

import (
	"crypto/hmac"
	"crypto/sha256"
	"encoding/base64"
	"encoding/json"
	"testing"
	"time"

	jwtv5 "github.com/golang-jwt/jwt/v5"
)

func init() {
	secretKey = []byte("test-secret-key-for-location-tests")
}

// helper: build a valid HS256 JWT using the jwt library (same as production).
func makeLocationTestJWT(t *testing.T, claims jwtv5.MapClaims) string {
	t.Helper()
	token := jwtv5.NewWithClaims(jwtv5.SigningMethodHS256, claims)
	signed, err := token.SignedString(secretKey)
	if err != nil {
		t.Fatalf("failed to sign token: %v", err)
	}
	return signed
}

// helper: build a JWT signed with stdlib (matching Django format).
func makeStdlibJWT(t *testing.T, claims map[string]interface{}, secret []byte) string {
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

func TestValidateToken_Valid(t *testing.T) {
	claims := jwtv5.MapClaims{
		"id":   "driver-uuid-abc",
		"type": "access_token",
		"exp":  float64(time.Now().Add(time.Hour).Unix()),
	}
	token := makeLocationTestJWT(t, claims)

	userID, err := validateToken(token)
	if err != nil {
		t.Fatalf("unexpected error: %v", err)
	}
	if userID != "driver-uuid-abc" {
		t.Errorf("expected driver-uuid-abc, got %s", userID)
	}
}

func TestValidateToken_Empty(t *testing.T) {
	_, err := validateToken("")
	if err == nil || err.Error() != "empty token" {
		t.Errorf("expected 'empty token' error, got: %v", err)
	}
}

func TestValidateToken_InvalidSignature(t *testing.T) {
	claims := map[string]interface{}{
		"id":   "driver-uuid",
		"type": "access_token",
		"exp":  float64(time.Now().Add(time.Hour).Unix()),
	}
	// Sign with wrong secret
	token := makeStdlibJWT(t, claims, []byte("wrong-secret"))

	_, err := validateToken(token)
	if err == nil {
		t.Error("expected error for invalid signature")
	}
}

func TestValidateToken_Expired(t *testing.T) {
	claims := jwtv5.MapClaims{
		"id":   "driver-uuid",
		"type": "access_token",
		"exp":  float64(time.Now().Add(-time.Hour).Unix()),
	}
	token := makeLocationTestJWT(t, claims)

	_, err := validateToken(token)
	if err == nil {
		t.Error("expected error for expired token")
	}
}

func TestValidateToken_WrongType(t *testing.T) {
	claims := jwtv5.MapClaims{
		"id":   "driver-uuid",
		"type": "refresh_token",
		"exp":  float64(time.Now().Add(time.Hour).Unix()),
	}
	token := makeLocationTestJWT(t, claims)

	_, err := validateToken(token)
	if err == nil || err.Error() != "wrong token type" {
		t.Errorf("expected 'wrong token type' error, got: %v", err)
	}
}

func TestValidateToken_MissingID(t *testing.T) {
	claims := jwtv5.MapClaims{
		"type": "access_token",
		"exp":  float64(time.Now().Add(time.Hour).Unix()),
	}
	token := makeLocationTestJWT(t, claims)

	_, err := validateToken(token)
	if err == nil || err.Error() != "token missing user id" {
		t.Errorf("expected 'token missing user id' error, got: %v", err)
	}
}

func TestValidateToken_NoExpiration(t *testing.T) {
	claims := jwtv5.MapClaims{
		"id":   "driver-uuid",
		"type": "access_token",
	}
	token := makeLocationTestJWT(t, claims)

	_, err := validateToken(token)
	if err == nil {
		t.Error("expected error for token without expiration")
	}
}

func TestValidateToken_WrongSigningMethod(t *testing.T) {
	// Create a token with "none" algorithm — should be rejected
	token := jwtv5.NewWithClaims(jwtv5.SigningMethodNone, jwtv5.MapClaims{
		"id":   "driver-uuid",
		"type": "access_token",
		"exp":  float64(time.Now().Add(time.Hour).Unix()),
	})
	signed, _ := token.SignedString(jwtv5.UnsafeAllowNoneSignatureType)

	_, err := validateToken(signed)
	if err == nil {
		t.Error("expected error for 'none' signing method")
	}
}
