#!/usr/bin/env python3
"""
Functional / integration tests for the TumaGo API.

These tests hit the running API through the gateway and verify end-to-end
behaviour. They require all services to be running (docker compose up).

Usage:
    cd backend
    docker compose up -d --build
    python -m pytest tests/functional_tests.py -v

Environment variables:
    BASE_URL  — gateway URL (default: http://localhost)
"""
import os
import time
import uuid

import requests
import pytest

BASE_URL = os.environ.get("BASE_URL", "http://localhost")


def url(path: str) -> str:
    return f"{BASE_URL}/{path.lstrip('/')}"


# ── Helpers ─────────────────────────────────────────────────────────────────


def signup_user(email=None, password="TestPass123"):
    if email is None:
        email = f"test-{uuid.uuid4().hex[:8]}@test.com"
    resp = requests.post(
        url("/signup/"),
        json={
            "email": email,
            "password": password,
            "phone_number": "0712345678",
            "name": "Func",
            "surname": "Test",
            "streetAdress": "1 St",
            "city": "JHB",
            "province": "GP",
            "postalCode": "2000",
        },
        timeout=10,
    )
    return resp, email


def signup_driver(email=None, password="DriverPass123"):
    if email is None:
        email = f"driver-{uuid.uuid4().hex[:8]}@test.com"
    resp = requests.post(
        url("/driver/signup/"),
        json={
            "email": email,
            "password": password,
            "name": "FuncDriver",
            "surname": "Test",
            "phone_number": "0700000000",
            "streetAdress": "1 St",
            "city": "CPT",
            "province": "WC",
            "postalCode": "8000",
        },
        timeout=10,
    )
    return resp, email


def auth_headers(token: str) -> dict:
    return {"Authorization": f"Bearer {token}"}


# ── Health checks ───────────────────────────────────────────────────────────


class TestHealthChecks:
    def test_gateway_health(self):
        resp = requests.get(url("/health"), timeout=5)
        assert resp.status_code == 200
        assert resp.json()["status"] == "ok"


# ── User signup & login flow ───────────────────────────────────────────────


class TestUserAuth:
    def test_signup_returns_tokens(self):
        resp, _ = signup_user()
        assert resp.status_code == 201
        data = resp.json()
        assert "accessToken" in data
        assert "refreshToken" in data

    def test_login_returns_tokens(self):
        _, email = signup_user()
        resp = requests.post(
            url("/login/"),
            json={"email": email, "password": "TestPass123"},
            timeout=10,
        )
        assert resp.status_code == 200
        assert "accessToken" in resp.json()

    def test_login_wrong_password(self):
        _, email = signup_user()
        resp = requests.post(
            url("/login/"),
            json={"email": email, "password": "wrong"},
            timeout=10,
        )
        assert resp.status_code == 400

    def test_duplicate_email_signup(self):
        _, email = signup_user()
        resp, _ = signup_user(email=email)
        assert resp.status_code == 400


# ── Token verification ──────────────────────────────────────────────────────


class TestTokenVerification:
    def test_valid_token(self):
        resp, _ = signup_user()
        token = resp.json()["accessToken"]
        verify_resp = requests.post(
            url("/verify_token/"), headers=auth_headers(token), timeout=10
        )
        assert verify_resp.status_code == 200

    def test_invalid_token(self):
        resp = requests.post(
            url("/verify_token/"),
            headers=auth_headers("invalid.token.here"),
            timeout=10,
        )
        assert resp.status_code in [401, 403]

    def test_no_token(self):
        resp = requests.post(url("/verify_token/"), timeout=10)
        assert resp.status_code in [401, 403]


# ── User profile ────────────────────────────────────────────────────────────


class TestUserProfile:
    def test_get_user_data(self):
        resp, _ = signup_user()
        token = resp.json()["accessToken"]
        data_resp = requests.post(
            url("/user/Data/"), headers=auth_headers(token), timeout=10
        )
        assert data_resp.status_code == 200
        assert "email" in data_resp.json()

    def test_update_profile(self):
        resp, _ = signup_user()
        token = resp.json()["accessToken"]
        update_resp = requests.post(
            url("/update/user/profile/"),
            headers=auth_headers(token),
            json={"name": "UpdatedName", "city": "Durban"},
            timeout=10,
        )
        assert update_resp.status_code == 200


# ── Trip expenses ───────────────────────────────────────────────────────────


class TestTripExpenses:
    def test_valid_distance(self):
        resp = requests.get(url("/trip_Expense/?distance=10"), timeout=10)
        assert resp.status_code == 200
        data = resp.json()
        assert "scooter" in data
        assert "van" in data
        assert "truck" in data
        # Verify pricing formula
        assert float(data["scooter"]) == pytest.approx(5.2, abs=0.01)
        assert float(data["van"]) == pytest.approx(11.3, abs=0.01)
        assert float(data["truck"]) == pytest.approx(23.5, abs=0.01)

    def test_missing_distance(self):
        resp = requests.get(url("/trip_Expense/"), timeout=10)
        assert resp.status_code == 400

    def test_invalid_distance(self):
        resp = requests.get(url("/trip_Expense/?distance=abc"), timeout=10)
        assert resp.status_code == 400

    def test_zero_distance(self):
        resp = requests.get(url("/trip_Expense/?distance=0"), timeout=10)
        assert resp.status_code == 400


# ── Sync time ───────────────────────────────────────────────────────────────


class TestSyncTime:
    def test_returns_utc_time(self):
        resp = requests.get(url("/sync/time/"), timeout=10)
        assert resp.status_code == 200
        assert "utc_time" in resp.json()


# ── Logout ──────────────────────────────────────────────────────────────────


class TestLogout:
    def test_logout_flow(self):
        resp, _ = signup_user()
        data = resp.json()
        token = data["accessToken"]
        refresh = data["refreshToken"]

        logout_resp = requests.post(
            url("/logout/"),
            headers=auth_headers(token),
            json={"refreshToken": refresh},
            timeout=10,
        )
        assert logout_resp.status_code == 200

    def test_logout_missing_refresh(self):
        resp, _ = signup_user()
        token = resp.json()["accessToken"]
        logout_resp = requests.post(
            url("/logout/"),
            headers=auth_headers(token),
            json={},
            timeout=10,
        )
        assert logout_resp.status_code == 400


# ── Terms & Conditions ──────────────────────────────────────────────────────


class TestTerms:
    def test_accept_and_verify_terms(self):
        resp, _ = signup_user()
        token = resp.json()["accessToken"]
        headers = auth_headers(token)

        # Accept
        accept_resp = requests.post(
            url("/accept/terms/"), headers=headers, timeout=10
        )
        assert accept_resp.status_code == 202

        # Verify
        verify_resp = requests.get(
            url("/verifyTerms/"), headers=headers, timeout=10
        )
        assert verify_resp.status_code == 200


# ── Driver signup & vehicle ─────────────────────────────────────────────────


class TestDriverFlow:
    def test_driver_signup(self):
        resp, _ = signup_driver()
        assert resp.status_code == 201
        assert "accessToken" in resp.json()

    def test_driver_add_vehicle(self):
        resp, _ = signup_driver()
        token = resp.json()["accessToken"]
        vehicle_resp = requests.post(
            url("/add/vehicle/"),
            headers=auth_headers(token),
            json={
                "delivery_vehicle": "van",
                "car_name": "Toyota Hilux",
                "number_plate": "GP 123 ABC",
                "color": "White",
                "vehicle_model": "2022",
            },
            timeout=10,
        )
        assert vehicle_resp.status_code == 201

    def test_driver_go_offline(self):
        resp, _ = signup_driver()
        token = resp.json()["accessToken"]
        offline_resp = requests.post(
            url("/driver/offline/"),
            headers=auth_headers(token),
            timeout=10,
        )
        assert offline_resp.status_code == 200


# ── Rate limiting (gateway) ────────────────────────────────────────────────


class TestRateLimiting:
    def test_auth_rate_limit(self):
        """Auth endpoints should be rate-limited at 10/min burst 5."""
        statuses = []
        for _ in range(8):
            resp = requests.post(
                url("/login/"),
                json={"email": "rate@test.com", "password": "pass"},
                timeout=10,
            )
            statuses.append(resp.status_code)
        # Should see at least one 429 after burst is exhausted
        assert 429 in statuses, f"Expected at least one 429, got: {statuses}"


# ── Delete account ──────────────────────────────────────────────────────────


class TestDeleteAccount:
    def test_delete_account(self):
        resp, _ = signup_user()
        token = resp.json()["accessToken"]
        del_resp = requests.delete(
            url("/delete/account/"),
            headers=auth_headers(token),
            timeout=10,
        )
        assert del_resp.status_code == 200

        # Verify token no longer works
        verify_resp = requests.post(
            url("/verify_token/"),
            headers=auth_headers(token),
            timeout=10,
        )
        assert verify_resp.status_code in [401, 403]
