"""
TumaGo Notification Service — FastAPI microservice.

Decouples FCM push notification delivery from the Django backend.
Consumes events from Redis pub/sub channels and sends FCM messages.

Channels listened to:
  - tumago:notify:driver   → send FCM to a driver
  - tumago:notify:client   → send FCM to a client

Event payload (JSON):
  {
    "fcm_token": "<token>",
    "title": "...",
    "body": "...",
    "data": { ... }
  }

Also exposes POST /send for direct HTTP calls from Django.
"""
import asyncio
import json
import logging
import os
from contextlib import asynccontextmanager

import redis.asyncio as aioredis
import uvicorn
from fastapi import FastAPI
from fastapi.middleware.cors import CORSMiddleware
from pydantic import BaseModel

import firebase_admin
from firebase_admin import credentials, messaging

logger = logging.getLogger(__name__)
logging.basicConfig(level=logging.INFO)

REDIS_URL = os.environ.get("REDIS_URL", "redis://redis:6379")
NOTIFY_CHANNELS = ["tumago:notify:driver", "tumago:notify:client"]

_firebase_initialised = False


def init_firebase():
    global _firebase_initialised
    if _firebase_initialised:
        return
    firebase_creds_path = os.environ.get("FIREBASE_CREDENTIALS_PATH", "")
    if firebase_creds_path and os.path.exists(firebase_creds_path):
        cred = credentials.Certificate(firebase_creds_path)
    else:
        # Fallback: Application Default Credentials (GCP / Cloud Run)
        cred = credentials.ApplicationDefault()
    firebase_admin.initialize_app(cred)
    _firebase_initialised = True
    logger.info("Firebase initialised")


async def consume_redis_events(redis_url: str):
    """Background task: subscribe to Redis pub/sub and dispatch FCM on each message.
    Reconnects automatically if the connection drops."""
    while True:
        try:
            redis_client = await aioredis.from_url(redis_url, decode_responses=True)
            pubsub = redis_client.pubsub()
            await pubsub.subscribe(*NOTIFY_CHANNELS)
            logger.info(f"Subscribed to Redis channels: {NOTIFY_CHANNELS}")

            async for message in pubsub.listen():
                if message["type"] != "message":
                    continue
                try:
                    payload = json.loads(message["data"])
                    loop = asyncio.get_running_loop()
                    await loop.run_in_executor(None, _send_fcm, payload)
                except Exception as e:
                    logger.error(f"Event processing error: {e}")

            # If listen() ends, the connection dropped — reconnect
            logger.warning("Redis pub/sub connection ended, reconnecting...")
        except asyncio.CancelledError:
            raise
        except Exception as e:
            logger.error(f"Redis consumer error: {e}, reconnecting in 3s...")
            await asyncio.sleep(3)


@asynccontextmanager
async def lifespan(app: FastAPI):
    init_firebase()
    redis_client = await aioredis.from_url(REDIS_URL, decode_responses=True)
    app.state.redis = redis_client

    # Start background pub/sub consumer as a task (manages its own connection for reconnect)
    task = asyncio.create_task(consume_redis_events(REDIS_URL))
    logger.info("Notification service started")
    yield
    task.cancel()
    try:
        await task
    except asyncio.CancelledError:
        pass
    await redis_client.aclose()
    logger.info("Notification service stopped")


app = FastAPI(title="TumaGo Notification Service", lifespan=lifespan)
app.add_middleware(CORSMiddleware, allow_origins=["*"], allow_methods=["POST", "GET"])


class NotificationPayload(BaseModel):
    fcm_token: str
    title: str
    body: str
    data: dict = {}


@app.get("/health")
async def health():
    return {"status": "ok"}


@app.post("/send")
async def send_notification(payload: NotificationPayload):
    """Direct HTTP endpoint — called by Django when pub/sub is unavailable."""
    loop = asyncio.get_running_loop()
    success = await loop.run_in_executor(None, _send_fcm, payload.model_dump())
    return {"success": success}


def _send_fcm(payload: dict) -> bool:
    """Synchronous FCM send — runs in thread pool to avoid blocking the event loop."""
    token = payload.get("fcm_token")
    if not token:
        return False

    str_data = {k: str(v) for k, v in payload.get("data", {}).items()}

    message = messaging.Message(
        token=token,
        notification=messaging.Notification(
            title=payload.get("title", ""),
            body=payload.get("body", ""),
        ),
        data=str_data,
    )
    try:
        response = messaging.send(message)
        logger.info(f"FCM sent: {response}")
        return True
    except Exception as e:
        logger.error(f"FCM error: {e}")
        return False


if __name__ == "__main__":
    uvicorn.run("main:app", host="0.0.0.0", port=8003, workers=1, log_level="info")
