"""
Redis-backed busy driver tracking.

Maintains a Redis SET 'busy_drivers' containing the string UUIDs of drivers
currently on a trip. Both the Go matching service and Django's
find_nearest_driver() check this set to skip busy drivers before hitting the DB.
"""
import logging
import redis
from django.conf import settings

logger = logging.getLogger(__name__)

BUSY_DRIVERS_KEY = "busy_drivers"

_redis_client = None


def _get_redis():
    global _redis_client
    if _redis_client is None:
        _redis_client = redis.Redis.from_url(settings.REDIS_URL, decode_responses=True)
    return _redis_client


def mark_driver_busy(driver_id):
    """Add a driver to the busy set (called when a driver accepts a trip)."""
    try:
        _get_redis().sadd(BUSY_DRIVERS_KEY, str(driver_id))
    except Exception as e:
        logger.error("Failed to mark driver %s busy: %s", driver_id, e)


def mark_driver_available(driver_id):
    """Remove a driver from the busy set (called when a trip ends or is cancelled)."""
    try:
        _get_redis().srem(BUSY_DRIVERS_KEY, str(driver_id))
    except Exception as e:
        logger.error("Failed to mark driver %s available: %s", driver_id, e)


def is_driver_busy(driver_id):
    """Check if a driver is in the busy set."""
    try:
        return _get_redis().sismember(BUSY_DRIVERS_KEY, str(driver_id))
    except Exception as e:
        logger.error("Failed to check busy status for driver %s: %s", driver_id, e)
        return False


def filter_busy_drivers(driver_ids):
    """Given a list of driver ID strings, return the subset that are NOT busy."""
    if not driver_ids:
        return driver_ids
    try:
        r = _get_redis()
        # SMISMEMBER returns a list of bools in the same order as the input
        flags = r.smismember(BUSY_DRIVERS_KEY, *driver_ids)
        return [did for did, is_busy in zip(driver_ids, flags) if not is_busy]
    except Exception as e:
        logger.error("Failed to filter busy drivers: %s", e)
        return driver_ids  # fail open — DB query will catch them
