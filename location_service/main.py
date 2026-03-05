"""
TumaGo Location Service — FastAPI WebSocket microservice.

Replaces Django Channels LocationConsumer for high-scale deployment.
Handles 10k–50k concurrent WebSocket connections using async uvicorn workers.

Architecture:
  - Drivers connect via ws://<host>/ws/driver_location?token=<JWT>
  - Each message upserts driver coords into Redis GEO set "driver_locations"
  - On disconnect, driver is removed from the GEO set
  - DB upsert happens in a thread pool (non-blocking)
"""
import json
import logging
from contextlib import asynccontextmanager

import redis.asyncio as aioredis
import uvicorn
from fastapi import FastAPI, WebSocket, WebSocketDisconnect, status
from fastapi.middleware.cors import CORSMiddleware

from auth import get_user_id_from_token
from db import upsert_driver_location, close_db_pool, init_db_pool

logger = logging.getLogger(__name__)
logging.basicConfig(level=logging.INFO)

# Redis GEO key — shared with the matching service
DRIVER_GEO_KEY = "driver_locations"


@asynccontextmanager
async def lifespan(app: FastAPI):
    # Startup
    await init_db_pool()
    app.state.redis = await aioredis.from_url(
        app.state.redis_url,
        encoding="utf-8",
        decode_responses=True,
        max_connections=50,
    )
    logger.info("Location service started")
    yield
    # Shutdown
    await app.state.redis.aclose()
    await close_db_pool()
    logger.info("Location service stopped")


app = FastAPI(title="TumaGo Location Service", lifespan=lifespan)
app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],
    allow_methods=["GET"],
    allow_headers=["*"],
)

import os
app.state.redis_url = os.environ.get("REDIS_URL", "redis://localhost:6379")


@app.get("/health")
async def health():
    return {"status": "ok"}


@app.websocket("/ws/driver_location")
async def driver_location_ws(websocket: WebSocket):
    """
    WebSocket endpoint for driver GPS streaming.
    Auth: JWT passed as query param `?token=<access_token>`
    """
    token = websocket.query_params.get("token")
    driver_id = get_user_id_from_token(token)

    if not driver_id:
        await websocket.close(code=status.WS_1008_POLICY_VIOLATION)
        return

    await websocket.accept()
    redis: aioredis.Redis = websocket.app.state.redis
    logger.info(f"Driver {driver_id} connected")

    try:
        async for raw in websocket.iter_text():
            try:
                data = json.loads(raw)
                lat = float(data["latitude"])
                lng = float(data["longitude"])
            except (json.JSONDecodeError, KeyError, ValueError):
                continue

            # 1. Update Redis GEO — O(log N), instant spatial queries
            await redis.geoadd(DRIVER_GEO_KEY, [lng, lat, driver_id])

            # 2. Upsert DB row — one row per driver (thread pool, non-blocking)
            await upsert_driver_location(driver_id, lat, lng)

    except WebSocketDisconnect:
        pass
    except Exception as e:
        logger.error(f"WebSocket error for driver {driver_id}: {e}")
    finally:
        # Remove driver from live GEO set on disconnect
        try:
            await redis.zrem(DRIVER_GEO_KEY, driver_id)
        except Exception:
            pass
        logger.info(f"Driver {driver_id} disconnected")


if __name__ == "__main__":
    uvicorn.run(
        "main:app",
        host="0.0.0.0",
        port=8001,
        workers=4,
        log_level="info",
    )
