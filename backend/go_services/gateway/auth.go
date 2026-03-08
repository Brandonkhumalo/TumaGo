package main

import (
	"crypto/hmac"
	"crypto/sha256"
	"encoding/base64"
	"encoding/json"
	"errors"
	"strings"
	"time"
)

// validateJWT performs lightweight HS256 JWT validation using only stdlib.
// It checks signature, expiry, and token type. Returns the user ID.
func validateJWT(tokenStr string) (string, error) {
	if tokenStr == "" {
		return "", errors.New("empty token")
	}

	parts := strings.Split(tokenStr, ".")
	if len(parts) != 3 {
		return "", errors.New("malformed token")
	}

	// Verify signature.
	signingInput := parts[0] + "." + parts[1]
	mac := hmac.New(sha256.New, secretKey)
	mac.Write([]byte(signingInput))
	expectedSig := base64.RawURLEncoding.EncodeToString(mac.Sum(nil))

	if !hmac.Equal([]byte(expectedSig), []byte(parts[2])) {
		return "", errors.New("invalid signature")
	}

	// Decode payload.
	payload, err := base64.RawURLEncoding.DecodeString(parts[1])
	if err != nil {
		return "", errors.New("invalid payload encoding")
	}

	var claims map[string]interface{}
	if err := json.Unmarshal(payload, &claims); err != nil {
		return "", errors.New("invalid payload JSON")
	}

	// Check expiry.
	exp, ok := claims["exp"].(float64)
	if !ok {
		return "", errors.New("token has no expiration")
	}
	if time.Now().UTC().Unix() > int64(exp) {
		return "", errors.New("token expired")
	}

	// Check token type.
	tokenType, _ := claims["type"].(string)
	if tokenType != "access_token" {
		return "", errors.New("wrong token type")
	}

	// Extract user ID.
	userID, _ := claims["id"].(string)
	if userID == "" {
		return "", errors.New("token missing user id")
	}

	return userID, nil
}
