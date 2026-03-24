"""
Dramatiq tasks for sending webhook notifications to B2B partners.

All webhooks are signed with HMAC-SHA256 using the partner's api_secret.
"""
import os
import django

os.environ.setdefault("DJANGO_SETTINGS_MODULE", "TumaGo.settings")
django.setup()

import hashlib
import hmac
import json
import logging

import dramatiq
import requests
from django.db import close_old_connections
from django.utils import timezone

from ...models import PartnerDeliveryRequest, DriverVehicle

logger = logging.getLogger(__name__)

WEBHOOK_TIMEOUT = 10  # seconds


def _sign_payload(body_bytes: bytes, secret: str) -> str:
    """HMAC-SHA256 signature of the request body."""
    return hmac.new(secret.encode(), body_bytes, hashlib.sha256).hexdigest()


def _build_driver_payload(delivery):
    """Build driver + vehicle info dict from a Delivery instance."""
    driver = delivery.driver
    payload = {
        "driver": {
            "name": f"{driver.name} {driver.surname}",
            "phone": driver.phone_number,
            "rating": float(driver.rating) if driver.rating else 0.0,
        },
    }

    vehicle = DriverVehicle.objects.filter(driver=driver).first()
    if vehicle:
        payload["vehicle"] = {
            "type": vehicle.delivery_vehicle,
            "name": vehicle.car_name,
            "color": vehicle.color,
            "number_plate": vehicle.number_plate,
            "model": vehicle.vehicle_model,
        }

    return payload


@dramatiq.actor(max_retries=3, min_backoff=5000, max_backoff=60000)
def send_partner_webhook(partner_delivery_id: str, event: str, extra_data: dict = None):
    """
    Send a webhook POST to the partner's webhook_url.

    Args:
        partner_delivery_id: UUID of the PartnerDeliveryRequest
        event: Event name (driver_assigned, location_update, picked_up, delivered, cancelled)
        extra_data: Additional fields to merge into the payload
    """
    close_old_connections()

    try:
        pdr = PartnerDeliveryRequest.objects.select_related(
            "partner", "delivery", "delivery__driver"
        ).get(id=partner_delivery_id)
    except PartnerDeliveryRequest.DoesNotExist:
        logger.warning("PartnerDeliveryRequest %s not found — skipping webhook", partner_delivery_id)
        return

    partner = pdr.partner
    if not partner.webhook_url:
        logger.info("Partner %s has no webhook_url — skipping", partner.name)
        return

    # Base payload — always includes event + partner_reference
    payload = {
        "event": event,
        "partner_reference": pdr.partner_reference,
        "delivery_id": str(pdr.delivery_id) if pdr.delivery_id else None,
        "timestamp": timezone.now().isoformat(),
    }

    # Event-specific data
    if event == "driver_assigned" and pdr.delivery:
        payload.update(_build_driver_payload(pdr.delivery))

    if event == "delivered" and pdr.delivery:
        payload["completed_at"] = pdr.delivery.end_time.isoformat() if pdr.delivery.end_time else None
        payload["fare"] = float(pdr.delivery.fare) if pdr.delivery.fare else 0

    if extra_data:
        payload.update(extra_data)

    body = json.dumps(payload)
    body_bytes = body.encode()
    signature = _sign_payload(body_bytes, partner.api_secret)

    headers = {
        "Content-Type": "application/json",
        "X-TumaGo-Signature": signature,
        "X-TumaGo-Event": event,
    }

    try:
        resp = requests.post(partner.webhook_url, data=body_bytes, headers=headers, timeout=WEBHOOK_TIMEOUT)
        logger.info(
            "Webhook %s sent to %s — status %d",
            event, partner.name, resp.status_code,
        )
        if resp.status_code >= 400:
            logger.warning("Webhook %s to %s returned %d: %s", event, partner.name, resp.status_code, resp.text[:200])
    except requests.RequestException as e:
        logger.error("Webhook %s to %s failed: %s", event, partner.name, e)
        raise  # Dramatiq will retry
