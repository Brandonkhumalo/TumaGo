"""Unit tests for JWT token generation and authentication."""
import time
from datetime import datetime, timedelta
from unittest.mock import MagicMock

import jwt
from django.conf import settings
from django.test import RequestFactory, TestCase
from jwt.exceptions import ExpiredSignatureError, InvalidTokenError

from TumaGo_Server.models import BlacklistedToken, CustomUser
from TumaGo_Server.token import JWTAuthentication
from rest_framework.exceptions import AuthenticationFailed


def make_user(**kwargs):
    defaults = {
        "email": "jwt@test.com",
        "password": "pass123",
        "name": "JWT",
        "surname": "Test",
        "phone_number": "0700000000",
        "streetAdress": "1 St",
        "city": "CPT",
        "province": "WC",
        "postalCode": "8000",
    }
    defaults.update(kwargs)
    return CustomUser.objects.create_user(**defaults)


class TokenGenerationTests(TestCase):
    def test_generate_access_token(self):
        payload = {"id": "test-uuid-123"}
        token = JWTAuthentication.generate_token(payload)
        decoded = jwt.decode(token, settings.SECRET_KEY, algorithms=["HS256"])
        self.assertEqual(decoded["id"], "test-uuid-123")
        self.assertEqual(decoded["type"], "access_token")
        self.assertIn("exp", decoded)

    def test_generate_refresh_token(self):
        payload = {"id": "test-uuid-456"}
        token = JWTAuthentication.generate_refresh_token(payload)
        decoded = jwt.decode(token, settings.SECRET_KEY, algorithms=["HS256"])
        self.assertEqual(decoded["id"], "test-uuid-456")
        self.assertEqual(decoded["type"], "refresh_token")

    def test_access_token_30_day_expiry(self):
        payload = {"id": "uuid"}
        token = JWTAuthentication.generate_token(payload)
        decoded = jwt.decode(token, settings.SECRET_KEY, algorithms=["HS256"])
        exp = datetime.utcfromtimestamp(decoded["exp"])
        now = datetime.utcnow()
        # Should expire roughly 30 days from now (allow 10 second tolerance)
        self.assertAlmostEqual((exp - now).days, 30, delta=1)

    def test_refresh_token_60_day_expiry(self):
        payload = {"id": "uuid"}
        token = JWTAuthentication.generate_refresh_token(payload)
        decoded = jwt.decode(token, settings.SECRET_KEY, algorithms=["HS256"])
        exp = datetime.utcfromtimestamp(decoded["exp"])
        now = datetime.utcnow()
        self.assertAlmostEqual((exp - now).days, 60, delta=1)

    def test_original_payload_not_mutated(self):
        payload = {"id": "uuid-original"}
        original_keys = set(payload.keys())
        JWTAuthentication.generate_token(payload)
        self.assertEqual(set(payload.keys()), original_keys)


class TokenVerificationTests(TestCase):
    def setUp(self):
        self.auth = JWTAuthentication()

    def test_valid_token_passes(self):
        payload = {
            "id": "uuid",
            "exp": int((datetime.utcnow() + timedelta(days=1)).timestamp()),
            "type": "access_token",
        }
        # Should not raise
        self.auth.verify_token(payload, token_type="access_token")

    def test_expired_token_raises(self):
        payload = {
            "id": "uuid",
            "exp": int((datetime.utcnow() - timedelta(days=1)).timestamp()),
            "type": "access_token",
        }
        with self.assertRaises(ExpiredSignatureError):
            self.auth.verify_token(payload)

    def test_missing_exp_raises(self):
        payload = {"id": "uuid", "type": "access_token"}
        with self.assertRaises(InvalidTokenError):
            self.auth.verify_token(payload)

    def test_wrong_token_type_raises(self):
        payload = {
            "id": "uuid",
            "exp": int((datetime.utcnow() + timedelta(days=1)).timestamp()),
            "type": "refresh_token",
        }
        with self.assertRaises(InvalidTokenError):
            self.auth.verify_token(payload, token_type="access_token")


class ExtractTokenTests(TestCase):
    def setUp(self):
        self.auth = JWTAuthentication()
        self.factory = RequestFactory()

    def test_extract_bearer_token(self):
        request = self.factory.get("/", HTTP_AUTHORIZATION="Bearer abc.def.ghi")
        token = self.auth.extract_token(request)
        self.assertEqual(token, "abc.def.ghi")

    def test_no_auth_header_returns_none(self):
        request = self.factory.get("/")
        token = self.auth.extract_token(request)
        self.assertIsNone(token)

    def test_non_bearer_returns_none(self):
        request = self.factory.get("/", HTTP_AUTHORIZATION="Basic abc123")
        token = self.auth.extract_token(request)
        self.assertIsNone(token)


class AuthenticateTests(TestCase):
    def setUp(self):
        self.auth = JWTAuthentication()
        self.factory = RequestFactory()
        self.user = make_user()

    def test_authenticate_valid_token(self):
        payload = {"id": str(self.user.id)}
        token = JWTAuthentication.generate_token(payload)
        request = self.factory.get("/", HTTP_AUTHORIZATION=f"Bearer {token}")
        user, returned_token = self.auth.authenticate(request)
        self.assertEqual(user.id, self.user.id)
        self.assertEqual(returned_token, token)

    def test_authenticate_blacklisted_token(self):
        payload = {"id": str(self.user.id)}
        token = JWTAuthentication.generate_token(payload)
        BlacklistedToken.objects.create(token=token)
        request = self.factory.get("/", HTTP_AUTHORIZATION=f"Bearer {token}")
        with self.assertRaises(AuthenticationFailed):
            self.auth.authenticate(request)

    def test_authenticate_no_token_returns_none(self):
        request = self.factory.get("/")
        result = self.auth.authenticate(request)
        self.assertIsNone(result)

    def test_authenticate_invalid_token_raises(self):
        request = self.factory.get("/", HTTP_AUTHORIZATION="Bearer invalid.token.here")
        with self.assertRaises(AuthenticationFailed):
            self.auth.authenticate(request)

    def test_authenticate_nonexistent_user_raises(self):
        payload = {"id": "00000000-0000-0000-0000-000000000000"}
        token = JWTAuthentication.generate_token(payload)
        request = self.factory.get("/", HTTP_AUTHORIZATION=f"Bearer {token}")
        with self.assertRaises(AuthenticationFailed):
            self.auth.authenticate(request)
