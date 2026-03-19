"""
Conversation state manager — stores multi-step flow state in Redis.

Each phone number has one active conversation state at a time.
State auto-expires after STATE_TTL seconds (30 minutes).
"""
import json
import logging
from typing import Optional

import redis.asyncio as aioredis

from config import REDIS_URL, STATE_TTL

logger = logging.getLogger(__name__)

_redis: Optional[aioredis.Redis] = None


async def get_redis() -> aioredis.Redis:
    global _redis
    if _redis is None:
        _redis = await aioredis.from_url(REDIS_URL, decode_responses=True)
    return _redis


async def close_redis():
    global _redis
    if _redis:
        await _redis.aclose()
        _redis = None


def _key(phone: str) -> str:
    return f"whatsapp:state:{phone}"


async def get_state(phone: str) -> Optional[dict]:
    """Get the current conversation state for a phone number."""
    r = await get_redis()
    raw = await r.get(_key(phone))
    if raw:
        return json.loads(raw)
    return None


async def set_state(phone: str, flow: str, step: str, data: dict | None = None):
    """Set or update conversation state."""
    r = await get_redis()
    state = {
        "flow": flow,
        "step": step,
        "data": data or {},
    }
    await r.set(_key(phone), json.dumps(state), ex=STATE_TTL)


async def update_data(phone: str, **kwargs):
    """Update data fields in the current state without changing flow/step."""
    state = await get_state(phone)
    if state:
        state["data"].update(kwargs)
        r = await get_redis()
        await r.set(_key(phone), json.dumps(state), ex=STATE_TTL)


async def clear_state(phone: str):
    """Remove conversation state (flow complete or cancelled)."""
    r = await get_redis()
    await r.delete(_key(phone))
