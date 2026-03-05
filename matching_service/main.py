"""
TumaGo Matching Service — FastAPI microservice.

Replaces the Django delivery.py matching logic.
Called by the Django backend via internal HTTP when a delivery is requested.

Flow:
  POST /match  →  GEOSEARCH Redis GEO  →  DB filter  →  return best driver
"""
import logging
import os
from typing import Optional

import redis
import asyncpg
from fastapi import FastAPI, HTTPException
from fastapi.middleware.cors import CORSMiddleware
from contextlib import asynccontextmanager
from pydantic import BaseModel

logger = logging.getLogger(__name__)
logging.basicConfig(level=logging.INFO)

DRIVER_GEO_KEY = "driver_locations"
REDIS_URL = os.environ.get("REDIS_URL", "redis://localhost:6379")
DATABASE_URL = os.environ.get("DATABASE_URL", "")
SEARCH_RADIUS_KM = float(os.environ.get("SEARCH_RADIUS_KM", "15"))

# Synchronous Redis for simple blocking ops in async context via thread pool
_redis_client: Optional[redis.Redis] = None
_db_pool: Optional[asyncpg.Pool] = None


def get_redis() -> redis.Redis:
    global _redis_client
    if _redis_client is None:
        _redis_client = redis.Redis.from_url(REDIS_URL, decode_responses=True)
    return _redis_client


@asynccontextmanager
async def lifespan(app: FastAPI):
    global _db_pool
    _db_pool = await asyncpg.create_pool(DATABASE_URL, min_size=5, max_size=20)
    logger.info("Matching service started")
    yield
    if _db_pool:
        await _db_pool.close()
    logger.info("Matching service stopped")


app = FastAPI(title="TumaGo Matching Service", lifespan=lifespan)
app.add_middleware(CORSMiddleware, allow_origins=["*"], allow_methods=["POST", "GET"])


class MatchRequest(BaseModel):
    origin_lat: float
    origin_lng: float
    vehicle_type: str
    trip_id: str


class MatchResponse(BaseModel):
    driver_id: str
    driver_name: str
    fcm_token: str
    driver_lat: float
    driver_lng: float
    distance_meters: float


@app.get("/health")
async def health():
    return {"status": "ok"}


@app.post("/match", response_model=Optional[MatchResponse])
async def match_driver(req: MatchRequest):
    """
    Find the closest available driver with matching vehicle type.
    Returns driver info or 404 if none found.
    """
    import asyncio
    loop = asyncio.get_event_loop()

    # Run Redis GEOSEARCH in thread pool (redis-py is synchronous)
    nearby = await loop.run_in_executor(None, _geo_search, req.origin_lat, req.origin_lng)

    if not nearby:
        raise HTTPException(status_code=404, detail="No drivers in radius")

    driver_ids_ordered = [entry[0] for entry in nearby]
    coords_map = {
        entry[0]: {"lat": entry[2][1], "lng": entry[2][0], "dist": entry[1] * 1000}
        for entry in nearby
    }

    # Single DB query — available drivers with matching vehicle
    driver = await _find_available_driver(driver_ids_ordered, req.vehicle_type, coords_map)

    if not driver:
        raise HTTPException(status_code=404, detail="No available drivers with that vehicle type")

    return driver


def _geo_search(lat: float, lng: float):
    """Run Redis GEOSEARCH (synchronous) — called via run_in_executor."""
    try:
        r = get_redis()
        return r.geosearch(
            DRIVER_GEO_KEY,
            longitude=lng,
            latitude=lat,
            radius=SEARCH_RADIUS_KM,
            unit="km",
            sort="ASC",
            withcoord=True,
            withdist=True,
        )
    except redis.RedisError as e:
        logger.error(f"Redis GEOSEARCH error: {e}")
        return []


async def _find_available_driver(
    driver_ids: list[str],
    vehicle_type: str,
    coords_map: dict,
) -> Optional[MatchResponse]:
    """Query DB for the first available driver with matching vehicle in proximity order."""
    if not _db_pool:
        return None

    placeholders = ",".join(f"${i+1}" for i in range(len(driver_ids)))
    query = f"""
        SELECT
            u.id::text,
            u.name,
            u.surname,
            u.fcm_token,
            u.driver_available,
            v.delivery_vehicle
        FROM "TumaGo_Server_customuser" u
        JOIN "TumaGo_Server_drivervehicle" v ON v.driver_id = u.id
        WHERE u.id::text = ANY(ARRAY[{placeholders}])
          AND u.driver_available = TRUE
          AND u.role = 'driver'
          AND v.delivery_vehicle = ${len(driver_ids) + 1}
    """
    params = driver_ids + [vehicle_type]

    async with _db_pool.acquire() as conn:
        rows = await conn.fetch(query, *params)

    drivers_map = {row["id"]: row for row in rows}

    for driver_id in driver_ids:
        row = drivers_map.get(driver_id)
        if row:
            coords = coords_map[driver_id]
            return MatchResponse(
                driver_id=driver_id,
                driver_name=f"{row['name']} {row['surname']}",
                fcm_token=row["fcm_token"] or "",
                driver_lat=coords["lat"],
                driver_lng=coords["lng"],
                distance_meters=coords["dist"],
            )
    return None
