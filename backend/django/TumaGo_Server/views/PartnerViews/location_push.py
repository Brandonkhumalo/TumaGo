"""
Periodic Dramatiq task that pushes live driver locations to B2B partners.

Runs every ~30 seconds. For each active PartnerDeliveryRequest with an
assigned driver, reads the driver's position from Redis and POSTs a
location_update webhook to the partner.
"""
import os
import django

os.environ.setdefault("DJANGO_SETTINGS_MODULE", "TumaGo.settings")
django.setup()

import logging

import dramatiq
import redis
from django.conf import settings
from django.db import close_old_connections

from ...models import PartnerDeliveryRequest
from .webhooks import send_partner_webhook

logger = logging.getLogger(__name__)


def _get_redis():
    return redis.Redis.from_url(settings.REDIS_URL, decode_responses=True)


@dramatiq.actor(max_retries=1, time_limit=25_000)
def push_partner_locations():
    """
    For each active partner delivery (status = driver_assigned or picked_up),
    read the driver's GPS from Redis and fire a location_update webhook.
    """
    close_old_connections()

    active_statuses = ("driver_assigned", "picked_up")
    pdrs = (
        PartnerDeliveryRequest.objects
        .filter(status__in=active_statuses, delivery__isnull=False)
        .select_related("delivery__driver", "partner")
    )

    if not pdrs.exists():
        return

    r = _get_redis()

    # Batch-read all driver positions in one GEOPOS call
    driver_ids = [str(pdr.delivery.driver_id) for pdr in pdrs]
    positions = r.geopos("driver_locations", *driver_ids)

    for pdr, pos in zip(pdrs, positions):
        if not pos:
            continue

        driver_loc = {"lat": pos[1], "lng": pos[0]}

        send_partner_webhook.send(
            str(pdr.id),
            "location_update",
            {
                "driver_location": driver_loc,
            },
        )

    logger.info("Pushed location updates for %d active partner deliveries", len(driver_ids))
