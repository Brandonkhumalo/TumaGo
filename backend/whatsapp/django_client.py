"""
HTTP client to call Django API endpoints.

The WhatsApp service never touches the database directly —
all business logic goes through Django's existing API endpoints.
"""
import logging
from typing import Optional

import httpx

from config import DJANGO_API_URL, SECRET_KEY

logger = logging.getLogger(__name__)

_client: Optional[httpx.AsyncClient] = None


def get_client() -> httpx.AsyncClient:
    global _client
    if _client is None or _client.is_closed:
        _client = httpx.AsyncClient(
            base_url=DJANGO_API_URL,
            timeout=15.0,
        )
    return _client


async def close_client():
    global _client
    if _client and not _client.is_closed:
        await _client.aclose()
        _client = None


def _auth_header(token: str) -> dict:
    """Build Authorization header from JWT token."""
    return {"Authorization": f"Bearer {token}"}


# ── Auth ─────────────────────────────────────────────────────────────────────

def _internal_header() -> dict:
    """Header that tells Django this is an internal WhatsApp call (skips OTP)."""
    return {"X-WhatsApp-Internal": SECRET_KEY}


async def signup(data: dict) -> dict:
    """Register a new client user. Includes internal header to skip OTP."""
    client = get_client()
    resp = await client.post("/signup/", json=data, headers=_internal_header())
    return {"status": resp.status_code, "data": resp.json()}


async def driver_signup(data: dict) -> dict:
    """Register a new driver. Includes internal header to skip OTP."""
    client = get_client()
    resp = await client.post("/driver/signup/", json=data, headers=_internal_header())
    return {"status": resp.status_code, "data": resp.json()}


async def login(email: str, password: str) -> dict:
    """Login and get JWT token."""
    client = get_client()
    resp = await client.post("/login/", json={"email": email, "password": password})
    return {"status": resp.status_code, "data": resp.json()}


# ── Delivery ─────────────────────────────────────────────────────────────────

async def get_fare_estimate(token: str, origin_lat: float, origin_lng: float,
                            dest_lat: float, dest_lng: float) -> dict:
    """Get fare estimates for all vehicle types."""
    client = get_client()
    resp = await client.post(
        "/trip_Expense/",
        json={
            "origin_lat": origin_lat,
            "origin_lng": origin_lng,
            "destination_lat": dest_lat,
            "destination_lng": dest_lng,
        },
        headers=_auth_header(token),
    )
    return {"status": resp.status_code, "data": resp.json()}


async def request_delivery(token: str, data: dict) -> dict:
    """Request a delivery."""
    client = get_client()
    resp = await client.post("/delivery/request/", json=data, headers=_auth_header(token))
    return {"status": resp.status_code, "data": resp.json()}


async def cancel_delivery(token: str, delivery_id: str) -> dict:
    """Cancel a delivery."""
    client = get_client()
    resp = await client.post(
        "/cancel/delivery/",
        json={"delivery_id": delivery_id},
        headers=_auth_header(token),
    )
    return {"status": resp.status_code, "data": resp.json()}


async def get_deliveries(token: str) -> dict:
    """Get delivery history."""
    client = get_client()
    resp = await client.get("/get/deliveries/", headers=_auth_header(token))
    return {"status": resp.status_code, "data": resp.json()}


async def track_delivery(token: str, delivery_id: str) -> dict:
    """Track a delivery."""
    client = get_client()
    resp = await client.get(
        "/track/delivery/",
        params={"delivery_id": delivery_id},
        headers=_auth_header(token),
    )
    return {"status": resp.status_code, "data": resp.json()}


# ── Driver ───────────────────────────────────────────────────────────────────

async def accept_trip(token: str, trip_id: str) -> dict:
    """Driver accepts a trip."""
    client = get_client()
    resp = await client.post(
        "/accept/trip/",
        json={"trip_id": trip_id},
        headers=_auth_header(token),
    )
    return {"status": resp.status_code, "data": resp.json()}


async def end_trip(token: str, delivery_id: str) -> dict:
    """Driver completes a delivery."""
    client = get_client()
    resp = await client.post(
        "/end_trip/",
        json={"delivery_id": delivery_id},
        headers=_auth_header(token),
    )
    return {"status": resp.status_code, "data": resp.json()}


async def driver_offline(token: str, online: bool) -> dict:
    """Set driver online/offline status."""
    client = get_client()
    resp = await client.post(
        "/driver/offline/",
        json={"online": online},
        headers=_auth_header(token),
    )
    return {"status": resp.status_code, "data": resp.json()}


async def add_vehicle(token: str, data: dict) -> dict:
    """Add driver vehicle."""
    client = get_client()
    resp = await client.post("/add/vehicle/", json=data, headers=_auth_header(token))
    return {"status": resp.status_code, "data": resp.json()}


async def upload_license(token: str, file_data: bytes, filename: str) -> dict:
    """Upload driver license image."""
    client = get_client()
    resp = await client.post(
        "/add/license/",
        files={"license_picture": (filename, file_data, "image/jpeg")},
        headers=_auth_header(token),
    )
    return {"status": resp.status_code, "data": resp.json()}


async def get_driver_finances(token: str) -> dict:
    """Get driver earnings and finances."""
    client = get_client()
    resp = await client.get("/driver/delivery_info/", headers=_auth_header(token))
    return {"status": resp.status_code, "data": resp.json()}


# ── Payment ──────────────────────────────────────────────────────────────────

async def initiate_payment(token: str, amount: float, payment_method: str,
                           phone: str = "") -> dict:
    """Start a Paynow payment."""
    client = get_client()
    data = {
        "amount": amount,
        "payment_method": payment_method,
    }
    if phone:
        data["phone"] = phone
    resp = await client.post("/payment/initiate/", json=data, headers=_auth_header(token))
    return {"status": resp.status_code, "data": resp.json()}


async def check_payment_status(token: str, payment_id: str) -> dict:
    """Check payment status."""
    client = get_client()
    resp = await client.get(
        "/payment/status/",
        params={"payment_id": payment_id},
        headers=_auth_header(token),
    )
    return {"status": resp.status_code, "data": resp.json()}


# ── Rating ───────────────────────────────────────────────────────────────────

async def rate_driver(token: str, delivery_id: str, rating: int) -> dict:
    """Rate a driver after delivery."""
    client = get_client()
    resp = await client.post(
        "/rate/driver/",
        json={"delivery_id": delivery_id, "rating": rating},
        headers=_auth_header(token),
    )
    return {"status": resp.status_code, "data": resp.json()}
