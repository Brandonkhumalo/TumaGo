"""
TumaGo WhatsApp Service — FastAPI microservice.

Handles:
  - Meta webhook verification (GET /webhook)
  - Incoming WhatsApp messages (POST /webhook)
  - Internal API for sending WhatsApp messages from other services
  - Redis pub/sub consumer for delivery event notifications
"""
import asyncio
import json
import logging
import os
from contextlib import asynccontextmanager

import uvicorn
from fastapi import FastAPI, Request, Response
from fastapi.middleware.cors import CORSMiddleware
from prometheus_fastapi_instrumentator import Instrumentator
from pydantic import BaseModel

from config import WHATSAPP_VERIFY_TOKEN
from webhook_handler import parse_webhook, is_status_update
from conversations.router import route_message
from event_consumer import consume_whatsapp_events
from sender import close_client as close_sender
from state import close_redis
from config import REDIS_URL

logger = logging.getLogger(__name__)
logging.basicConfig(level=logging.INFO)


@asynccontextmanager
async def lifespan(app: FastAPI):
    """Start background Redis event consumer on startup, cleanup on shutdown."""
    task = asyncio.create_task(consume_whatsapp_events(REDIS_URL))
    logger.info("WhatsApp service started")
    yield
    task.cancel()
    try:
        await task
    except asyncio.CancelledError:
        pass
    await close_sender()
    await close_redis()
    logger.info("WhatsApp service stopped")


app = FastAPI(title="TumaGo WhatsApp Service", lifespan=lifespan)
app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],
    allow_methods=["POST", "GET"],
)

Instrumentator().instrument(app).expose(app)


# ── Health check ─────────────────────────────────────────────────────────────

@app.get("/health")
async def health():
    return {"status": "ok"}


# ── Webhook verification (Meta sends GET to verify) ─────────────────────────

@app.get("/webhook")
async def verify_webhook(request: Request):
    """
    Meta sends a GET request with hub.mode, hub.verify_token, and hub.challenge.
    We must echo back hub.challenge if the token matches.
    """
    mode = request.query_params.get("hub.mode", "")
    token = request.query_params.get("hub.verify_token", "")
    challenge = request.query_params.get("hub.challenge", "")

    if mode == "subscribe" and token == WHATSAPP_VERIFY_TOKEN:
        logger.info("WhatsApp webhook verified")
        return Response(content=challenge, media_type="text/plain")

    logger.warning(f"Webhook verification failed: mode={mode}")
    return Response(content="Forbidden", status_code=403)


# ── Incoming messages (Meta sends POST) ──────────────────────────────────────

@app.post("/webhook")
async def receive_webhook(request: Request):
    """
    Meta POSTs incoming messages, button replies, media, and status updates.
    Must respond 200 within 5 seconds — process async.
    """
    try:
        payload = await request.json()
    except Exception:
        return {"status": "ok"}

    # Ignore status updates (delivered, read receipts)
    if is_status_update(payload):
        return {"status": "ok"}

    # Parse and process messages in background (don't block the 200 response)
    messages = parse_webhook(payload)
    for msg in messages:
        asyncio.create_task(_process_message(msg))

    return {"status": "ok"}


async def _process_message(msg):
    """Process a single incoming message — runs as background task."""
    try:
        logger.info(f"Message from {msg.phone}: type={msg.msg_type} text='{msg.text}'")
        await route_message(msg)
    except Exception as e:
        logger.exception(f"Error processing message from {msg.phone}: {e}")


# ── Internal API (called by other services) ──────────────────────────────────

class SendMessageRequest(BaseModel):
    to: str
    template: str | None = None
    text: str | None = None
    body_params: list[str] | None = None
    button_params: list[dict] | None = None


@app.post("/internal/send")
async def internal_send(req: SendMessageRequest):
    """
    Internal endpoint — called by Django/other services to send WhatsApp messages.
    Not exposed externally (gateway blocks /internal/ paths).
    """
    from sender import send_template, send_text

    if req.template:
        result = await send_template(req.to, req.template, req.body_params, req.button_params)
    elif req.text:
        result = await send_text(req.to, req.text)
    else:
        return {"error": "Provide 'template' or 'text'"}

    return result


# ── Publish event helper (for other services to trigger WhatsApp via Redis) ──

class PublishEventRequest(BaseModel):
    channel: str
    phone: str
    data: dict = {}


@app.post("/internal/publish")
async def internal_publish(req: PublishEventRequest):
    """
    Publish an event to the WhatsApp Redis pub/sub channels.
    Other services can call this instead of publishing to Redis directly.
    """
    from state import get_redis
    r = await get_redis()
    payload = json.dumps({"phone": req.phone, "data": req.data})
    await r.publish(req.channel, payload)
    return {"status": "published"}


if __name__ == "__main__":
    uvicorn.run("main:app", host="0.0.0.0", port=8004, workers=1, log_level="info")
