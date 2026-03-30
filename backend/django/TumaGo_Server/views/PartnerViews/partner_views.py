"""
B2B Partner API endpoints.

All endpoints authenticate via X-API-Key header (PartnerAPIKeyAuthentication).
request.auth is the PartnerCompany instance; request.user is None.
"""
import redis
from decimal import Decimal
from rest_framework.decorators import api_view, authentication_classes, permission_classes
from rest_framework.permissions import AllowAny
from rest_framework.response import Response
from rest_framework import status
from django.conf import settings

from ...partner_auth import PartnerAPIKeyAuthentication
from ...models import (
    PartnerCompany,
    PartnerDeliveryRequest,
    PartnerTransaction,
    TripRequest,
    CustomUser,
    DriverVehicle,
)
from ..DriverViews.deliveryMatching.tasks import retry_trip_matching
from ...busy_drivers import mark_driver_available

import logging

logger = logging.getLogger(__name__)

# ---------------------------------------------------------------------------
# Helpers
# ---------------------------------------------------------------------------

def _get_partner(request):
    """Extract and validate the PartnerCompany from request.auth."""
    partner = request.auth
    if not isinstance(partner, PartnerCompany):
        return None
    return partner


def _get_redis():
    return redis.Redis.from_url(settings.REDIS_URL, decode_responses=True)


def _get_driver_location(driver_id: str):
    """Read latest driver position from Redis GEO set."""
    try:
        r = _get_redis()
        pos = r.geopos("driver_locations", driver_id)
        if pos and pos[0]:
            return {"lat": pos[0][1], "lng": pos[0][0]}
    except Exception as e:
        logger.error("Redis geopos error: %s", e)
    return None


# ---------------------------------------------------------------------------
# Shared permission setup — all partner endpoints use the same pattern
# ---------------------------------------------------------------------------

PARTNER_AUTH = [PartnerAPIKeyAuthentication]
PARTNER_PERM = [AllowAny]  # Auth is handled by the API key class itself


# ---------------------------------------------------------------------------
# 1. POST /api/v1/partner/delivery/request — Request a delivery
# ---------------------------------------------------------------------------

@api_view(["POST"])
@authentication_classes(PARTNER_AUTH)
@permission_classes(PARTNER_PERM)
def partner_request_delivery(request):
    partner = _get_partner(request)
    if not partner:
        return Response({"detail": "Authentication required."}, status=status.HTTP_401_UNAUTHORIZED)

    # Check partner is active
    if not partner.is_active:
        return Response(
            {"detail": "Partner account is not active."},
            status=status.HTTP_403_FORBIDDEN,
        )

    data = request.data
    required = ["origin_lat", "origin_lng", "destination_lat", "destination_lng", "vehicle"]
    missing = [f for f in required if f not in data]
    if missing:
        return Response(
            {"detail": f"Missing required fields: {', '.join(missing)}"},
            status=status.HTTP_400_BAD_REQUEST,
        )

    # Validate fare — required for partner deliveries so we can deduct from balance
    fare = data.get("fare", 0)
    try:
        fare = float(fare)
    except (ValueError, TypeError):
        fare = 0
    if fare <= 0:
        return Response(
            {"detail": "Fare is required for partner deliveries"},
            status=status.HTTP_400_BAD_REQUEST,
        )

    # Check partner has sufficient balance
    fare_decimal = Decimal(str(fare))
    if partner.balance < fare_decimal:
        return Response(
            {
                "detail": f"Insufficient balance. Current balance: ${float(partner.balance):.2f}, delivery fare: ${fare:.2f}"
            },
            status=status.HTTP_402_PAYMENT_REQUIRED,
        )

    # We need a system user to act as the "requester" for the TripRequest.
    # Use or create a dedicated partner system account.
    system_user, _ = CustomUser.objects.get_or_create(
        email=f"partner+{partner.id}@system.tumago.internal",
        defaults={
            "name": partner.name,
            "surname": "Partner",
            "phone_number": "0000000000",
            "streetAdress": "N/A",
            "city": "N/A",
            "province": "N/A",
            "postalCode": "0000",
            "role": CustomUser.USER,
        },
    )

    # Build delivery_details in the same shape the Android app sends
    delivery_details = {
        "origin_lat": float(data["origin_lat"]),
        "origin_lng": float(data["origin_lng"]),
        "destination_lat": float(data["destination_lat"]),
        "destination_lng": float(data["destination_lng"]),
        "vehicle": data["vehicle"],
        "fare": float(data.get("fare", 0)),
        "payment_method": data.get("payment_method", "account"),
    }

    # Create TripRequest (same model the Android app uses)
    trip = TripRequest.objects.create(
        requester=system_user,
        delivery_details=delivery_details,
    )

    # Create PartnerDeliveryRequest to track this partner delivery
    pdr = PartnerDeliveryRequest.objects.create(
        partner=partner,
        partner_reference=data.get("partner_reference", ""),
        trip_request=trip,
        status="matching",
        pickup_contact=data.get("pickup_contact", ""),
        dropoff_contact=data.get("dropoff_contact", ""),
        package_description=data.get("package_description", ""),
    )

    # Deduct fare from partner balance
    partner.balance -= fare_decimal
    partner.save(update_fields=['balance'])

    # Record the delivery charge transaction
    PartnerTransaction.objects.create(
        partner=partner,
        transaction_type='delivery_charge',
        amount=-fare_decimal,
        balance_after=partner.balance,
        description=f"Delivery charge for {pdr.partner_reference or 'delivery'}",
        delivery_request=pdr,
        fare=fare_decimal,
    )

    # Fire existing matching task — same flow as the Android app
    try:
        retry_trip_matching.send(str(trip.id), str(system_user.id), delivery_details)
    except Exception as e:
        logger.error("Failed to start matching task for partner delivery: %s", e)

    return Response(
        {
            "id": str(pdr.id),
            "partner_reference": pdr.partner_reference,
            "status": "matching",
            "trip_id": str(trip.id),
        },
        status=status.HTTP_201_CREATED,
    )


# ---------------------------------------------------------------------------
# 2. GET /api/v1/partner/delivery/<id>/status — Poll delivery status
# ---------------------------------------------------------------------------

@api_view(["GET"])
@authentication_classes(PARTNER_AUTH)
@permission_classes(PARTNER_PERM)
def partner_delivery_status(request, delivery_id):
    partner = _get_partner(request)
    if not partner:
        return Response({"detail": "Authentication required."}, status=status.HTTP_401_UNAUTHORIZED)

    try:
        pdr = PartnerDeliveryRequest.objects.select_related(
            "trip_request", "delivery", "delivery__driver"
        ).get(id=delivery_id, partner=partner)
    except PartnerDeliveryRequest.DoesNotExist:
        return Response({"detail": "Delivery not found."}, status=status.HTTP_404_NOT_FOUND)

    response_data = {
        "id": str(pdr.id),
        "partner_reference": pdr.partner_reference,
        "status": pdr.status,
        "created_at": pdr.created_at.isoformat(),
    }

    # Add origin/destination from trip details
    if pdr.trip_request:
        details = pdr.trip_request.delivery_details
        response_data["origin"] = {
            "lat": details.get("origin_lat"),
            "lng": details.get("origin_lng"),
        }
        response_data["destination"] = {
            "lat": details.get("destination_lat"),
            "lng": details.get("destination_lng"),
        }
        response_data["fare"] = details.get("fare")

    # Add driver + vehicle info if a driver has been assigned
    if pdr.delivery and pdr.delivery.driver:
        driver = pdr.delivery.driver
        response_data["driver"] = {
            "name": f"{driver.name} {driver.surname}",
            "rating": float(driver.rating) if driver.rating else 0.0,
            "phone": driver.phone_number,
        }

        vehicle = DriverVehicle.objects.filter(driver=driver).first()
        if vehicle:
            response_data["vehicle"] = {
                "type": vehicle.delivery_vehicle,
                "name": vehicle.car_name,
                "color": vehicle.color,
                "number_plate": vehicle.number_plate,
                "model": vehicle.vehicle_model,
            }

        # Live driver location from Redis
        driver_loc = _get_driver_location(str(driver.id))
        if driver_loc:
            response_data["driver_location"] = driver_loc

    # Add completion data if delivered
    if pdr.delivery and pdr.delivery.end_time:
        response_data["completed_at"] = pdr.delivery.end_time.isoformat()

    return Response(response_data, status=status.HTTP_200_OK)


# ---------------------------------------------------------------------------
# 3. POST /api/v1/partner/delivery/<id>/cancel — Cancel a delivery
# ---------------------------------------------------------------------------

@api_view(["POST"])
@authentication_classes(PARTNER_AUTH)
@permission_classes(PARTNER_PERM)
def partner_cancel_delivery(request, delivery_id):
    from ..DriverViews.deliveryMatching.tasks import publish_trip_cancelled

    partner = _get_partner(request)
    if not partner:
        return Response({"detail": "Authentication required."}, status=status.HTTP_401_UNAUTHORIZED)

    try:
        pdr = PartnerDeliveryRequest.objects.select_related(
            "trip_request", "delivery", "delivery__driver"
        ).get(id=delivery_id, partner=partner)
    except PartnerDeliveryRequest.DoesNotExist:
        return Response({"detail": "Delivery not found."}, status=status.HTTP_404_NOT_FOUND)

    if pdr.status in ("delivered", "cancelled"):
        return Response(
            {"detail": f"Cannot cancel — delivery is already {pdr.status}."},
            status=status.HTTP_400_BAD_REQUEST,
        )

    # Still matching — cancel the TripRequest and signal the matching task
    if pdr.status == "matching" and pdr.trip_request and not pdr.trip_request.accepted:
        pdr.trip_request.cancelled = True
        pdr.trip_request.save()
        publish_trip_cancelled(str(pdr.trip_request.id))

    # Driver already assigned — cancel the Delivery and free the driver
    if pdr.delivery:
        pdr.delivery.successful = False
        pdr.delivery.save()

        driver = pdr.delivery.driver
        if driver:
            driver.driver_available = True
            driver.save()
            mark_driver_available(driver.id)

            # Notify driver via FCM
            try:
                from firebase_admin import messaging
                if driver.fcm_token:
                    msg = messaging.Message(
                        token=driver.fcm_token,
                        data={"type": "delivery_cancelled"},
                        notification=messaging.Notification(
                            title="Delivery Cancelled",
                            body=f"Partner delivery {pdr.partner_reference} was cancelled.",
                        ),
                    )
                    messaging.send(msg)
            except Exception as e:
                logger.error("FCM cancel notification error: %s", e)

    pdr.status = "cancelled"
    pdr.save()

    # Refund the fare back to partner balance
    fare = 0
    if pdr.trip_request:
        fare = pdr.trip_request.delivery_details.get("fare", 0)
    if fare and float(fare) > 0:
        fare_decimal = Decimal(str(fare))
        partner.balance += fare_decimal
        partner.save(update_fields=['balance'])

        PartnerTransaction.objects.create(
            partner=partner,
            transaction_type='refund',
            amount=fare_decimal,
            balance_after=partner.balance,
            description=f"Refund for cancelled delivery {pdr.partner_reference}",
            delivery_request=pdr,
            fare=fare_decimal,
        )

    # Fire webhook to partner
    from .webhooks import send_partner_webhook
    send_partner_webhook.send(str(pdr.id), "cancelled", {"reason": "partner_cancelled"})

    return Response({"detail": "Delivery cancelled.", "status": "cancelled"}, status=status.HTTP_200_OK)


# ---------------------------------------------------------------------------
# 4. GET /api/v1/partner/deliveries — List partner deliveries (paginated)
# ---------------------------------------------------------------------------

@api_view(["GET"])
@authentication_classes(PARTNER_AUTH)
@permission_classes(PARTNER_PERM)
def partner_list_deliveries(request):
    partner = _get_partner(request)
    if not partner:
        return Response({"detail": "Authentication required."}, status=status.HTTP_401_UNAUTHORIZED)

    # Simple offset pagination
    try:
        page = int(request.query_params.get("page", 1))
        page_size = int(request.query_params.get("page_size", 20))
    except (ValueError, TypeError):
        page, page_size = 1, 20
    page_size = min(page_size, 100)  # Cap at 100

    qs = PartnerDeliveryRequest.objects.filter(partner=partner).order_by("-created_at")
    total = qs.count()
    offset = (page - 1) * page_size
    items = qs[offset : offset + page_size]

    results = []
    for pdr in items:
        results.append({
            "id": str(pdr.id),
            "partner_reference": pdr.partner_reference,
            "status": pdr.status,
            "created_at": pdr.created_at.isoformat(),
            "delivery_id": str(pdr.delivery_id) if pdr.delivery_id else None,
        })

    return Response(
        {
            "count": total,
            "page": page,
            "page_size": page_size,
            "results": results,
        },
        status=status.HTTP_200_OK,
    )


# ---------------------------------------------------------------------------
# 5. GET /api/v1/partner/balance — Check partner balance
# ---------------------------------------------------------------------------

@api_view(["GET"])
@authentication_classes(PARTNER_AUTH)
@permission_classes(PARTNER_PERM)
def partner_check_balance(request):
    partner = _get_partner(request)
    if not partner:
        return Response({"detail": "Authentication required."}, status=status.HTTP_401_UNAUTHORIZED)

    return Response({
        "balance": float(partner.balance),
        "setup_fee_paid": partner.setup_fee_paid,
        "commission_rate": float(partner.commission_rate),
    })
