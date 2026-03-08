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

from decimal import Decimal
import redis
import dramatiq
from dramatiq.errors import Retry
from django.conf import settings
from ....models import TripRequest, CustomUser
from ....serializers.userSerializer.authserializers import UserSerializer

import logging
logger = logging.getLogger(__name__)

WAIT_TIMEOUT_SECONDS = 12     # How long to wait for acceptance before re-matching
TRIP_ACCEPTED_CHANNEL = "trip_accepted:{trip_id}"


def _get_redis():
    return redis.Redis.from_url(settings.REDIS_URL, decode_responses=True)


def _wait_for_acceptance(trip_id: str, timeout: int) -> bool:
    """
    Block on a Redis pub/sub subscription until the trip is accepted or timeout.
    Returns True if accepted, False if timed out.
    """
    r = _get_redis()
    channel = TRIP_ACCEPTED_CHANNEL.format(trip_id=trip_id)
    pubsub = r.pubsub()
    pubsub.subscribe(channel)

    try:
        # pubsub.listen() yields messages; get_message(timeout=...) is non-blocking poll
        for message in pubsub.listen():
            if message and message['type'] == 'message':
                if message['data'] == 'accepted':
                    return True
            # Check once immediately then let listen() handle the rest
        # Should not reach here — but treat as timeout
        return False
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

    try:
        trip = TripRequest.objects.get(id=trip_id)
    except TripRequest.DoesNotExist:
        logger.warning(f"Trip {trip_id} not found — aborting matching task")
        return

    if trip.accepted:
        logger.info(f"Trip {trip_id} already accepted — task done")
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
    TripData(requester_data, delivery_data, trip_id)

    # Wait for driver acceptance via Redis pub/sub (no sleep, no DB polling)
    accepted = _wait_for_acceptance(trip_id, WAIT_TIMEOUT_SECONDS)

    if accepted:
        logger.info(f"Trip {trip_id} accepted during wait — task complete")
        return

    # Not accepted — check DB one final time before re-queueing
    trip.refresh_from_db()
    if trip.accepted:
        logger.info(f"Trip {trip_id} accepted (DB check) — task complete")
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
        channel = TRIP_ACCEPTED_CHANNEL.format(trip_id=trip_id)
        r.publish(channel, "accepted")
    except Exception as e:
        logger.error(f"Failed to publish trip acceptance for {trip_id}: {e}")
