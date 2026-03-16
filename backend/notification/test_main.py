"""Unit tests for the notification service."""
import json
from unittest.mock import MagicMock, patch

import pytest
from fastapi.testclient import TestClient


# Mock firebase_admin before importing main so init_firebase doesn't fail.
@pytest.fixture(autouse=True)
def mock_firebase(monkeypatch):
    mock_cred = MagicMock()
    monkeypatch.setattr("firebase_admin.credentials.Certificate", lambda p: mock_cred)
    monkeypatch.setattr("firebase_admin.credentials.ApplicationDefault", lambda: mock_cred)
    monkeypatch.setattr("firebase_admin.initialize_app", MagicMock())
    # Reset the global flag so each test gets a fresh state
    import main as m
    m._firebase_initialised = False


@pytest.fixture
def client():
    """Create a test client without the lifespan (no Redis needed)."""
    from main import app
    return TestClient(app, raise_server_exceptions=False)


# ── Health endpoint ─────────────────────────────────────────────────────────


def test_health(client):
    resp = client.get("/health")
    assert resp.status_code == 200
    assert resp.json() == {"status": "ok"}


# ── POST /send ──────────────────────────────────────────────────────────────


@patch("main._send_fcm")
def test_send_notification_success(mock_send, client):
    mock_send.return_value = True
    payload = {
        "fcm_token": "test-token-123",
        "title": "New Delivery",
        "body": "You have a new delivery request",
        "data": {"type": "new_request", "trip_id": "abc"},
    }
    resp = client.post("/send", json=payload)
    assert resp.status_code == 200
    assert resp.json()["success"] is True
    mock_send.assert_called_once()


@patch("main._send_fcm")
def test_send_notification_failure(mock_send, client):
    mock_send.return_value = False
    payload = {
        "fcm_token": "bad-token",
        "title": "Test",
        "body": "Test body",
    }
    resp = client.post("/send", json=payload)
    assert resp.status_code == 200
    assert resp.json()["success"] is False


def test_send_notification_missing_fields(client):
    resp = client.post("/send", json={"title": "Missing token"})
    assert resp.status_code == 422  # Pydantic validation error


def test_send_notification_empty_data(client):
    with patch("main._send_fcm", return_value=True):
        payload = {
            "fcm_token": "token",
            "title": "Title",
            "body": "Body",
        }
        resp = client.post("/send", json=payload)
        assert resp.status_code == 200


# ── _send_fcm unit tests ───────────────────────────────────────────────────


@patch("main.messaging")
def test_send_fcm_success(mock_messaging):
    mock_messaging.send.return_value = "projects/test/messages/123"
    from main import _send_fcm

    result = _send_fcm({
        "fcm_token": "valid-token",
        "title": "Hello",
        "body": "World",
        "data": {"key": "value"},
    })
    assert result is True
    mock_messaging.send.assert_called_once()


@patch("main.messaging")
def test_send_fcm_exception(mock_messaging):
    mock_messaging.send.side_effect = Exception("FCM error")
    from main import _send_fcm

    result = _send_fcm({
        "fcm_token": "token",
        "title": "Hello",
        "body": "World",
    })
    assert result is False


def test_send_fcm_missing_token():
    from main import _send_fcm

    result = _send_fcm({"title": "No token"})
    assert result is False


def test_send_fcm_empty_token():
    from main import _send_fcm

    result = _send_fcm({"fcm_token": "", "title": "Empty"})
    assert result is False


@patch("main.messaging")
def test_send_fcm_data_values_stringified(mock_messaging):
    mock_messaging.send.return_value = "ok"
    from main import _send_fcm

    _send_fcm({
        "fcm_token": "token",
        "title": "T",
        "body": "B",
        "data": {"count": 42, "active": True},
    })
    # Verify the Message was constructed with stringified data values
    call_args = mock_messaging.Message.call_args
    str_data = call_args.kwargs.get("data", {})
    assert str_data["count"] == "42"
    assert str_data["active"] == "True"


# ── NotificationPayload model ──────────────────────────────────────────────


def test_notification_payload_defaults():
    from main import NotificationPayload

    p = NotificationPayload(fcm_token="tok", title="T", body="B")
    assert p.data == {}


def test_notification_payload_with_data():
    from main import NotificationPayload

    p = NotificationPayload(
        fcm_token="tok", title="T", body="B", data={"key": "val"}
    )
    assert p.data == {"key": "val"}
