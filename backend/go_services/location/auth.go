package main

import (
	"errors"
	"os"
	"time"

	jwtv5 "github.com/golang-jwt/jwt/v5"
)

var secretKey []byte

func initAuth() {
	secretKey = []byte(os.Getenv("SECRET_KEY"))
}

// validateToken decodes an HS256 JWT and returns the driver UUID string.
// Returns an error if the token is missing, expired, has the wrong type, or is
// otherwise invalid.
func validateToken(tokenStr string) (string, error) {
	if tokenStr == "" {
		return "", errors.New("empty token")
	}

	token, err := jwtv5.Parse(tokenStr, func(t *jwtv5.Token) (interface{}, error) {
		if _, ok := t.Method.(*jwtv5.SigningMethodHMAC); !ok {
			return nil, errors.New("unexpected signing method")
		}
		return secretKey, nil
	})
	if err != nil {
		return "", err
	}

	claims, ok := token.Claims.(jwtv5.MapClaims)
	if !ok || !token.Valid {
		return "", errors.New("invalid token claims")
	}

	// Check expiry (the library checks "exp" automatically, but the Python
	// code also does a manual check; belt-and-suspenders).
	exp, err := claims.GetExpirationTime()
	if err != nil || exp == nil {
		return "", errors.New("token has no expiration")
	}
	if time.Now().UTC().After(exp.Time) {
		return "", errors.New("token expired")
	}

	// Token type must be "access_token".
	tokenType, _ := claims["type"].(string)
	if tokenType != "access_token" {
		return "", errors.New("wrong token type")
	}

	// Extract the user/driver UUID.
	userID, _ := claims["id"].(string)
	if userID == "" {
		return "", errors.New("token missing user id")
	}

	return userID, nil
}
