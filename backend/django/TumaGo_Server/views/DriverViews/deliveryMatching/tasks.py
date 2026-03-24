"""
Dramatiq background tasks for driver matching.

Old approach: time.sleep(1) loop polling DB every second — blocks worker threads.
New approach:
  - Subscribe to Redis pub/sub channel "trip_accepted:<trip_id>"
  - AcceptTrip view publishes to that channel when a driver accepts
  - Task waits on the channel with a timeout — no busy-waiting, no sleep
"""
import os
import django

os.environ.setdefault("DJANGO_SETTINGS_MODULE", "TumaGo.settings")
django.setup()

import time
from decimal import Decimal
import redis
import dramatiq
from dramatiq.errors import Retry
from django.conf import settings
from django.db import close_old_connections
from ....models import TripRequest, CustomUser
from ....serializers.userSerializer.authserializers import UserSerializer

import logging
logger = logging.getLogger(__name__)

WAIT_TIMEOUT_SECONDS = 12     # How long to wait for acceptance before re-matching
TRIP_CHANNEL = "trip_signal:{trip_id}"


def _get_redis():
    return redis.Redis.from_url(settings.REDIS_URL, decode_responses=True)


def _wait_for_signal(trip_id: str, timeout: int) -> str:
    """
    Block on a Redis pub/sub subscription until the trip is accepted, cancelled, or timeout.
    Returns 'accepted', 'cancelled', or 'timeout'.
    """
    r = _get_redis()
    channel = TRIP_CHANNEL.format(trip_id=trip_id)
    pubsub = r.pubsub()
    pubsub.subscribe(channel)

    try:
        deadline = time.time() + timeout
        while time.time() < deadline:
            message = pubsub.get_message(timeout=1)
            if message and message['type'] == 'message':
                if message['data'] in ('accepted', 'cancelled'):
                    return message['data']
        return 'timeout'
    finally:
        try:
            pubsub.unsubscribe(channel)
            pubsub.close()
        except Exception:
            pass


def convert_to_decimal(obj):
    if isinstance(obj, list):
        return [convert_to_decimal(i) for i in obj]
    elif isinstance(obj, dict):
        return {k: convert_to_decimal(v) for k, v in obj.items()}
    elif isinstance(obj, float):
        return Decimal(str(obj))
    return obj


@dramatiq.actor(max_retries=4, time_limit=60_000)
def retry_trip_matching(trip_id: str, user_id: str, delivery_data: dict):
    """
    Background task that:
      1. Runs TripData() to find a driver and send them an FCM notification.
      2. Waits WAIT_TIMEOUT_SECONDS for the driver to accept (via Redis pub/sub).
      3. If accepted → done.
      4. If not accepted → re-queues itself (up to max_retries).
      5. On final retry exhaustion → notifies user no drivers found.
    """
    from ..deliveryMatching.delivery import TripData, no_driver_found

    # Dramatiq workers are long-lived and don't go through Django's
    # request/response cycle, so stale DB connections are never cleaned up.
    # Close them before any DB access to avoid "connection already closed".
    close_old_connections()

    try:
        trip = TripRequest.objects.get(id=trip_id)
    except TripRequest.DoesNotExist:
        logger.warning(f"Trip {trip_id} not found — aborting matching task")
        return

    if trip.accepted:
        logger.info(f"Trip {trip_id} already accepted — task done")
        return

    if trip.cancelled:
        logger.info(f"Trip {trip_id} already cancelled — task done")
        return

    try:
        user = CustomUser.objects.get(id=user_id)
    except CustomUser.DoesNotExist:
        logger.warning(f"User {user_id} not found — aborting matching task")
        return

    # Convert floats back to Decimal before passing to TripData
    delivery_data = convert_to_decimal(delivery_data)
    requester_data = UserSerializer(user).data

    logger.info(f"Running TripData matching for trip {trip_id}")
    result = TripData(requester_data, delivery_data, trip_id)

    if result is None:
        # No drivers available — no point waiting or retrying
        logger.info(f"No drivers found for trip {trip_id} — notifying user")
        no_driver_found(user)
        return

    # A driver was found and notified — wait for acceptance or cancellation via Redis pub/sub
    signal = _wait_for_signal(trip_id, WAIT_TIMEOUT_SECONDS)

    if signal == 'accepted':
        logger.info(f"Trip {trip_id} accepted during wait — task complete")
        return

    if signal == 'cancelled':
        logger.info(f"Trip {trip_id} cancelled during wait — task complete")
        return

    # Connection may have gone stale during the pub/sub wait
    close_old_connections()

    # Not accepted — check DB one final time before re-queueing
    trip.refresh_from_db()
    if trip.accepted:
        logger.info(f"Trip {trip_id} accepted (DB check) — task complete")
        return

    if trip.cancelled:
        logger.info(f"Trip {trip_id} cancelled (DB check) — task complete")
        return

    logger.info(f"Trip {trip_id} not accepted, re-queueing")
    raise Retry(delay=2000)  # Re-queue after 2 s


def publish_trip_accepted(trip_id: str):
    """
    Called by AcceptTrip view after a driver accepts.
    Signals the waiting retry_trip_matching task to stop.
    """
    try:
        r = _get_redis()
        channel = TRIP_CHANNEL.format(trip_id=trip_id)
        r.publish(channel, "accepted")
    except Exception as e:
        logger.error(f"Failed to publish trip acceptance for {trip_id}: {e}")


def publish_trip_cancelled(trip_id: str):
    """
    Called by cancel_trip_request view when the user cancels.
    Signals the waiting retry_trip_matching task to stop.
    """
    try:
        r = _get_redis()
        channel = TRIP_CHANNEL.format(trip_id=trip_id)
        r.publish(channel, "cancelled")
    except Exception as e:
        logger.error(f"Failed to publish trip cancellation for {trip_id}: {e}")
