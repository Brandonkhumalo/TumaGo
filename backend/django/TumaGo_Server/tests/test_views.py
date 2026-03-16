"""Unit tests for Django views (API endpoints)."""
import json
from decimal import Decimal
from unittest.mock import patch

from django.test import TestCase
from django.utils import timezone
from rest_framework.test import APIClient

from TumaGo_Server.models import (
    BlacklistedToken,
    CustomUser,
    Delivery,
    DriverVehicle,
    TermsAndConditions,
)
from TumaGo_Server.token import JWTAuthentication


def make_user(email="user@test.com", role=CustomUser.USER, **kwargs):
    defaults = {
        "password": "testpass123",
        "name": "Test",
        "surname": "User",
        "phone_number": "0712345678",
        "streetAdress": "123 Main",
        "city": "Joburg",
        "province": "Gauteng",
        "postalCode": "2000",
    }
    defaults.update(kwargs)
    return CustomUser.objects.create_user(email=email, role=role, **defaults)


def auth_client(user):
    """Return an APIClient with a valid access token for the given user."""
    client = APIClient()
    token = JWTAuthentication.generate_token({"id": str(user.id)})
    client.credentials(HTTP_AUTHORIZATION=f"Bearer {token}")
    return client


# ── Signup / Login ──────────────────────────────────────────────────────────


class SignupTests(TestCase):
    def test_signup_success(self):
        client = APIClient()
        resp = client.post(
            "/signup/",
            {
                "email": "new@test.com",
                "password": "SecurePass1",
                "phone_number": "0700000000",
                "name": "New",
                "surname": "User",
                "streetAdress": "1 St",
                "city": "CPT",
                "province": "WC",
                "postalCode": "8000",
            },
            format="json",
        )
        self.assertEqual(resp.status_code, 201)
        self.assertIn("accessToken", resp.data)
        self.assertIn("refreshToken", resp.data)

    def test_signup_missing_email(self):
        client = APIClient()
        resp = client.post("/signup/", {"password": "pass"}, format="json")
        self.assertEqual(resp.status_code, 400)

    def test_signup_duplicate_email(self):
        make_user(email="dup@test.com")
        client = APIClient()
        resp = client.post(
            "/signup/",
            {
                "email": "dup@test.com",
                "password": "pass",
                "phone_number": "071",
                "name": "A",
                "surname": "B",
                "streetAdress": "x",
                "city": "x",
                "province": "x",
                "postalCode": "x",
            },
            format="json",
        )
        self.assertEqual(resp.status_code, 400)


class LoginTests(TestCase):
    def test_login_success(self):
        make_user(email="login@test.com")
        client = APIClient()
        resp = client.post(
            "/login/",
            {"email": "login@test.com", "password": "testpass123"},
            format="json",
        )
        self.assertEqual(resp.status_code, 200)
        self.assertIn("accessToken", resp.data)

    def test_login_wrong_password(self):
        make_user(email="login2@test.com")
        client = APIClient()
        resp = client.post(
            "/login/",
            {"email": "login2@test.com", "password": "wrong"},
            format="json",
        )
        self.assertEqual(resp.status_code, 400)

    def test_login_nonexistent_email(self):
        client = APIClient()
        resp = client.post(
            "/login/",
            {"email": "nope@test.com", "password": "pass"},
            format="json",
        )
        self.assertEqual(resp.status_code, 400)


# ── Auth-protected endpoints ────────────────────────────────────────────────


class VerifyTokenTests(TestCase):
    def test_valid_token_returns_200(self):
        user = make_user()
        client = auth_client(user)
        resp = client.post("/verify_token/")
        self.assertEqual(resp.status_code, 200)

    def test_no_token_returns_401(self):
        client = APIClient()
        resp = client.post("/verify_token/")
        # DRF returns 401 for unauthenticated
        self.assertIn(resp.status_code, [401, 403])


class LogoutTests(TestCase):
    def test_logout_blacklists_refresh_token(self):
        user = make_user()
        client = auth_client(user)
        refresh = JWTAuthentication.generate_refresh_token({"id": str(user.id)})
        resp = client.post("/logout/", {"refreshToken": refresh}, format="json")
        self.assertEqual(resp.status_code, 200)
        self.assertTrue(BlacklistedToken.objects.filter(token=refresh).exists())

    def test_logout_without_refresh_token(self):
        user = make_user()
        client = auth_client(user)
        resp = client.post("/logout/", {}, format="json")
        self.assertEqual(resp.status_code, 400)


# ── Profile ─────────────────────────────────────────────────────────────────


class UpdateProfileTests(TestCase):
    def test_update_name(self):
        user = make_user()
        client = auth_client(user)
        resp = client.post(
            "/update/user/profile/",
            {"name": "NewName"},
            format="json",
        )
        self.assertEqual(resp.status_code, 200)
        user.refresh_from_db()
        self.assertEqual(user.name, "NewName")


class GetUserDataTests(TestCase):
    def test_returns_user_data(self):
        user = make_user()
        client = auth_client(user)
        resp = client.post("/user/Data/")
        self.assertEqual(resp.status_code, 200)
        self.assertEqual(resp.data["email"], user.email)


# ── Trip Expenses ───────────────────────────────────────────────────────────


class TripExpensesTests(TestCase):
    def test_valid_distance(self):
        client = APIClient()
        resp = client.get("/trip_Expense/", {"distance": "10"})
        self.assertEqual(resp.status_code, 200)
        self.assertIn("scooter", resp.data)
        self.assertIn("van", resp.data)
        self.assertIn("truck", resp.data)

    def test_pricing_formula(self):
        client = APIClient()
        resp = client.get("/trip_Expense/", {"distance": "10"})
        # scooter: 0.50*10 + 0.20 = 5.20
        self.assertEqual(float(resp.data["scooter"]), 5.2)
        # van: 1.10*10 + 0.30 = 11.30
        self.assertEqual(float(resp.data["van"]), 11.3)
        # truck: 2.30*10 + 0.50 = 23.50
        self.assertEqual(float(resp.data["truck"]), 23.5)

    def test_missing_distance(self):
        client = APIClient()
        resp = client.get("/trip_Expense/")
        self.assertEqual(resp.status_code, 400)

    def test_invalid_distance(self):
        client = APIClient()
        resp = client.get("/trip_Expense/", {"distance": "abc"})
        self.assertEqual(resp.status_code, 400)

    def test_negative_distance(self):
        client = APIClient()
        resp = client.get("/trip_Expense/", {"distance": "-5"})
        self.assertEqual(resp.status_code, 400)

    def test_zero_distance(self):
        client = APIClient()
        resp = client.get("/trip_Expense/", {"distance": "0"})
        self.assertEqual(resp.status_code, 400)


# ── Cancel Delivery ─────────────────────────────────────────────────────────


class CancelDeliveryTests(TestCase):
    @patch("TumaGo_Server.views.UserViews.userViews.messaging")
    def test_cancel_marks_unsuccessful(self, mock_messaging):
        client_user = make_user(email="c@test.com")
        driver = make_user(email="d@test.com", role=CustomUser.DRIVER, fcm_token="token123")
        delivery = Delivery.objects.create(
            driver=driver,
            client=client_user,
            start_time=timezone.now(),
            fare=Decimal("20.00"),
            payment_method="cash",
        )
        api_client = auth_client(client_user)
        resp = api_client.post(f"/cancel/delivery/?delivery_id={delivery.delivery_id}")
        self.assertEqual(resp.status_code, 200)
        delivery.refresh_from_db()
        self.assertFalse(delivery.successful)
        driver.refresh_from_db()
        self.assertTrue(driver.driver_available)

    def test_cancel_nonexistent_delivery(self):
        user = make_user()
        client = auth_client(user)
        resp = client.post("/cancel/delivery/?delivery_id=00000000-0000-0000-0000-000000000000")
        self.assertEqual(resp.status_code, 404)

    def test_cancel_missing_delivery_id(self):
        user = make_user()
        client = auth_client(user)
        resp = client.post("/cancel/delivery/")
        self.assertEqual(resp.status_code, 400)


# ── Terms & Conditions ──────────────────────────────────────────────────────


class TermsTests(TestCase):
    def test_accept_terms(self):
        user = make_user()
        client = auth_client(user)
        resp = client.post("/accept/terms/")
        self.assertEqual(resp.status_code, 202)
        self.assertTrue(
            TermsAndConditions.objects.filter(user=user, terms_and_conditions=True).exists()
        )

    def test_check_terms_accepted(self):
        user = make_user()
        TermsAndConditions.objects.create(user=user, terms_and_conditions=True)
        client = auth_client(user)
        resp = client.get("/verifyTerms/")
        self.assertEqual(resp.status_code, 200)

    def test_check_terms_not_accepted(self):
        user = make_user()
        TermsAndConditions.objects.create(user=user, terms_and_conditions=False)
        client = auth_client(user)
        resp = client.get("/verifyTerms/")
        self.assertEqual(resp.status_code, 403)


# ── Sync Time ───────────────────────────────────────────────────────────────


class SyncTimeTests(TestCase):
    def test_returns_utc_time(self):
        client = APIClient()
        resp = client.get("/sync/time/")
        self.assertEqual(resp.status_code, 200)
        self.assertIn("utc_time", resp.data)


# ── Admin/Dev endpoints ─────────────────────────────────────────────────────


class AdminEndpointTests(TestCase):
    def test_get_all_users(self):
        make_user()
        client = APIClient()
        resp = client.get("/allUsers/")
        self.assertEqual(resp.status_code, 200)
        self.assertEqual(len(resp.data), 1)

    def test_get_all_deliveries(self):
        client = APIClient()
        resp = client.get("/allDeliveries/")
        self.assertEqual(resp.status_code, 200)

    def test_delete_all_users(self):
        make_user()
        client = APIClient()
        resp = client.delete("/delete/")
        self.assertEqual(resp.status_code, 200)
        self.assertEqual(CustomUser.objects.count(), 0)
