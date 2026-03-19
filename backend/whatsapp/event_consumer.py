"""
Redis pub/sub event consumer — listens for delivery events from Django/Dramatiq
and sends WhatsApp messages to drivers and clients.

Channels:
  - tumago:whatsapp:driver_matched    → notify driver of new delivery
  - tumago:whatsapp:driver_found      → notify client that driver accepted
  - tumago:whatsapp:no_driver         → notify client no driver available
  - tumago:whatsapp:driver_at_pickup  → notify client driver arrived
  - tumago:whatsapp:en_route          → notify client driver picked up package
  - tumago:whatsapp:delivery_complete → notify client + driver delivery done
  - tumago:whatsapp:payment_confirmed → notify client payment received

Event payload (JSON):
  {
    "phone": "263771234567",
    "event": "driver_matched",
    "data": { ... event-specific data ... }
  }
"""
import asyncio
import json
import logging

import redis.asyncio as aioredis

from config import REDIS_URL
from conversations.driver_delivery import (
    notify_driver_new_delivery,
    notify_client_driver_found,
    notify_client_no_driver,
    notify_client_driver_at_pickup,
    notify_client_en_route,
    notify_client_delivery_complete,
)
from sender import send_text

logger = logging.getLogger(__name__)

WHATSAPP_CHANNELS = [
    "tumago:whatsapp:driver_matched",
    "tumago:whatsapp:driver_found",
    "tumago:whatsapp:no_driver",
    "tumago:whatsapp:driver_at_pickup",
    "tumago:whatsapp:en_route",
    "tumago:whatsapp:delivery_complete",
    "tumago:whatsapp:payment_confirmed",
]


async def consume_whatsapp_events(redis_url: str):
    """
    Background task: subscribe to Redis pub/sub channels for WhatsApp events.
    Reconnects automatically if connection drops.
    """
    while True:
        try:
            redis_client = await aioredis.from_url(redis_url, decode_responses=True)
            pubsub = redis_client.pubsub()
            await pubsub.subscribe(*WHATSAPP_CHANNELS)
            logger.info(f"WhatsApp event consumer subscribed to: {WHATSAPP_CHANNELS}")

            async for message in pubsub.listen():
                if message["type"] != "message":
                    continue
                try:
                    payload = json.loads(message["data"])
                    await _dispatch_event(message["channel"], payload)
                except Exception as e:
                    logger.error(f"WhatsApp event processing error: {e}")

            logger.warning("Redis pub/sub connection ended, reconnecting...")
        except asyncio.CancelledError:
            raise
        except Exception as e:
            logger.error(f"WhatsApp consumer error: {e}, reconnecting in 3s...")
            await asyncio.sleep(3)


async def _dispatch_event(channel: str, payload: dict):
    """Route a Redis event to the appropriate WhatsApp handler."""
    phone = payload.get("phone", "")
    data = payload.get("data", {})

    if not phone:
        logger.warning(f"Event on {channel} missing phone number")
        return

    logger.info(f"WhatsApp event: {channel} → {phone}")

    if channel == "tumago:whatsapp:driver_matched":
        await notify_driver_new_delivery(
            driver_phone=phone,
            trip_id=data.get("trip_id", ""),
            pickup=data.get("pickup", ""),
            dropoff=data.get("dropoff", ""),
            distance_km=float(data.get("distance_km", 0)),
            fare=float(data.get("fare", 0)),
            package=data.get("package", "Package"),
            payment_method=data.get("payment_method", "cash"),
        )

    elif channel == "tumago:whatsapp:driver_found":
        await notify_client_driver_found(
            client_phone=phone,
            delivery_id=data.get("delivery_id", ""),
            driver_name=data.get("driver_name", ""),
            vehicle_type=data.get("vehicle_type", ""),
            vehicle_name=data.get("vehicle_name", ""),
            color=data.get("color", ""),
            number_plate=data.get("number_plate", ""),
            eta_minutes=int(data.get("eta_minutes", 0)),
            driver_phone=data.get("driver_phone", ""),
        )

    elif channel == "tumago:whatsapp:no_driver":
        await notify_client_no_driver(phone)

    elif channel == "tumago:whatsapp:driver_at_pickup":
        await notify_client_driver_at_pickup(phone, data.get("driver_name", ""))

    elif channel == "tumago:whatsapp:en_route":
        await notify_client_en_route(phone, data.get("driver_name", ""))

    elif channel == "tumago:whatsapp:delivery_complete":
        await notify_client_delivery_complete(
            phone, data.get("driver_name", ""), data.get("delivery_id", ""),
        )

    elif channel == "tumago:whatsapp:payment_confirmed":
        amount = data.get("amount", "0.00")
        method = data.get("payment_method", "")
        reference = data.get("reference", "")
        await send_text(
            phone,
            f"Payment confirmed! ✅\n\n"
            f"Amount: ${amount}\n"
            f"Method: {method.upper()}\n"
            f"Reference: {reference}\n\n"
            "Finding a driver for you now...",
        )
