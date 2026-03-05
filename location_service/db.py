"""
Async PostgreSQL helper for the location service.
Uses asyncpg connection pool — no Django ORM overhead.
"""
import os
import asyncio
import logging
from concurrent.futures import ThreadPoolExecutor

import asyncpg

logger = logging.getLogger(__name__)

_pool: asyncpg.Pool | None = None
_executor = ThreadPoolExecutor(max_workers=4)

DATABASE_URL = os.environ.get("DATABASE_URL", "")


async def init_db_pool():
    global _pool
    _pool = await asyncpg.create_pool(
        DATABASE_URL,
        min_size=5,
        max_size=20,
        command_timeout=10,
    )
    logger.info("DB pool initialised")


async def close_db_pool():
    if _pool:
        await _pool.close()


async def upsert_driver_location(driver_id: str, lat: float, lng: float):
    """
    Upsert the DriverLocations row (one row per driver).
    The table is: tumago_server_driverlocations (driver_id, latitude, longitude)
    """
    if not _pool:
        return
    try:
        async with _pool.acquire() as conn:
            await conn.execute(
                """
                INSERT INTO "TumaGo_Server_driverlocations" (driver_id, latitude, longitude)
                VALUES ($1, $2, $3)
                ON CONFLICT (driver_id)
                DO UPDATE SET latitude = EXCLUDED.latitude, longitude = EXCLUDED.longitude
                """,
                driver_id, str(lat), str(lng),
            )
    except Exception as e:
        logger.error(f"DB upsert error for driver {driver_id}: {e}")
