from rest_framework.response import Response
from rest_framework import status
from rest_framework.decorators import api_view, permission_classes
from rest_framework.permissions import IsAuthenticated, AllowAny
from ...serializers.userSerializer.authserializers import UserSerializer
from decimal import Decimal
from ...models import Delivery, CustomUser, TripRequest, PartnerDeliveryRequest, DriverLocations, DriverWallet, WalletTransaction, PlatformSettings
from firebase_admin import messaging
from TumaGo.firebase_init import initialize_firebase
from django.shortcuts import get_object_or_404
from ...busy_drivers import mark_driver_available
from django.db import transaction as db_transaction
import logging
logger = logging.getLogger(__name__)

initialize_firebase()

@api_view(["POST"])
@permission_classes([IsAuthenticated])
def GetUserData(request):
    user = request.user
    serializer = UserSerializer(user)
    return Response(serializer.data, status=status.HTTP_200_OK)

@api_view(["GET"])
@permission_classes([AllowAny])
def GetTripExpenses(request):
    try:
        distance = request.query_params.get("distance", None)

        if distance is None:
            return Response({"error": "Distance parameter is required."}, status=status.HTTP_400_BAD_REQUEST)

        try:
            distance = float(distance)
        except ValueError:
            return Response({"error": "Invalid distance value."}, status=status.HTTP_400_BAD_REQUEST)

        if distance <= 0:
            return Response({"error": "Distance must be greater than 0."}, status=status.HTTP_400_BAD_REQUEST)

        dist = Decimal(str(distance))
        s = PlatformSettings.load()
        scooterPrice = round(s.scooter_price_per_km * dist + s.scooter_base_fee, 2)
        vanPrice = round(s.van_price_per_km * dist + s.van_base_fee, 2)
        truckPrice = round(s.truck_price_per_km * dist + s.truck_base_fee, 2)

        fare = {
            "scooter": scooterPrice,
            "van": vanPrice,
            "truck": truckPrice
        }

        return Response(fare, status=status.HTTP_200_OK)

    except (TypeError, ValueError):
        return Response({"error": "Invalid distance or time parameters."}, status=status.HTTP_400_BAD_REQUEST)
    
@api_view(["POST"])
@permission_classes([IsAuthenticated])
def cancel_delivery(request):
    user = request.user
    delivery_id = request.query_params.get("delivery_id")

    if not delivery_id:
        return Response({"error": "delivery_id is required."}, status=status.HTTP_400_BAD_REQUEST)

    try:
        delivery = Delivery.objects.get(delivery_id=delivery_id)

        if delivery.picked_up:
            return Response(
                {"error": "Cannot cancel — driver has already picked up the package."},
                status=status.HTTP_400_BAD_REQUEST,
            )

        delivery.successful = False
        delivery.save()

        driver = delivery.driver
        driver.driver_available = True
        driver.save()
        mark_driver_available(driver.id)

        # Auto-refund commission to driver wallet (client cancelled)
        commission_txn = WalletTransaction.objects.filter(
            driver=driver,
            delivery=delivery,
            transaction_type=WalletTransaction.COMMISSION,
        ).first()
        if commission_txn:
            refund_amount = abs(commission_txn.amount)
            with db_transaction.atomic():
                wallet = DriverWallet.objects.select_for_update().get(driver=driver)
                wallet.balance += refund_amount
                wallet.save()
                WalletTransaction.objects.create(
                    driver=driver,
                    transaction_type=WalletTransaction.REFUND,
                    amount=refund_amount,
                    balance_after=wallet.balance,
                    delivery=delivery,
                    description="Commission refunded — client cancelled delivery",
                )
            logger.info(f"Refunded ${refund_amount} to driver {driver.email} wallet (client cancel)")

        client = delivery.client
        client_name = client.name
        client_surname = client.surname

        update_driver_delivery_cancelled(driver, client_name, client_surname)

        # B2B: If this delivery is linked to a partner, update status + fire webhook
        partner_req = PartnerDeliveryRequest.objects.filter(delivery=delivery).first()
        if partner_req:
            partner_req.status = "cancelled"
            partner_req.save()
            from ..PartnerViews.webhooks import send_partner_webhook
            send_partner_webhook.send(str(partner_req.id), "cancelled", {"reason": "driver_cancelled"})

        return Response({"message": "Delivery cancelled."}, status=status.HTTP_200_OK)
    except Delivery.DoesNotExist:
        return Response({"error": "Delivery not found."}, status=status.HTTP_404_NOT_FOUND)
    
def update_driver_delivery_cancelled(driver, name, surname):
    if not driver.fcm_token:
        return 
    
    message = messaging.Message(
        token=driver.fcm_token,
        data={
            "type": "delivery_cancelled",
        },
        notification=messaging.Notification(
            title="Delivery Cancelled",
            body=f"by {name} {surname}",
        )
    )

    try:
        response = messaging.send(message)
        logger.info(f"FCM cancellation sent to driver {driver.id}: {response}")
        return True
    except Exception as e:
        logger.error(f"FCM cancellation error for driver {driver.id}: {e}")
        return False
    
@api_view(["POST"])
@permission_classes([IsAuthenticated])
def cancel_trip_request(request):
    """Cancel a pending trip request before a driver accepts it."""
    from ..DriverViews.deliveryMatching.tasks import publish_trip_cancelled

    trip_id = request.data.get("trip_id")
    if not trip_id:
        return Response({"error": "trip_id is required."}, status=status.HTTP_400_BAD_REQUEST)

    try:
        trip = TripRequest.objects.get(id=trip_id, requester=request.user)
    except TripRequest.DoesNotExist:
        return Response({"error": "Trip request not found."}, status=status.HTTP_404_NOT_FOUND)

    if trip.accepted:
        return Response({"error": "Trip already accepted — use cancel delivery instead."},
                        status=status.HTTP_400_BAD_REQUEST)

    trip.cancelled = True
    trip.save()

    # Signal the background matching task to stop immediately
    publish_trip_cancelled(str(trip.id))

    logger.info("Trip request %s cancelled by user %s", trip_id, request.user.id)
    return Response({"message": "Trip request cancelled."}, status=status.HTTP_200_OK)


@api_view(["POST"])
@permission_classes([IsAuthenticated])
def driver_cancel_delivery(request):
    """Driver cancels an active delivery. Requires a reason for refund review.

    Body: { "delivery_id": "<uuid>", "reason": "Vehicle broke down" }
    """
    from ...models import CommissionRefundRequest

    user = request.user
    if user.role != CustomUser.DRIVER:
        return Response({"error": "Only drivers can use this endpoint"},
                        status=status.HTTP_403_FORBIDDEN)

    delivery_id = request.data.get("delivery_id")
    reason = request.data.get("reason", "").strip()

    if not delivery_id:
        return Response({"error": "delivery_id is required"}, status=status.HTTP_400_BAD_REQUEST)
    if not reason:
        return Response({"error": "reason is required for driver cancellations"},
                        status=status.HTTP_400_BAD_REQUEST)

    try:
        delivery = Delivery.objects.get(delivery_id=delivery_id, driver=user)
    except Delivery.DoesNotExist:
        return Response({"error": "Delivery not found"}, status=status.HTTP_404_NOT_FOUND)

    if delivery.picked_up:
        return Response({"error": "Cannot cancel — package already picked up"},
                        status=status.HTTP_400_BAD_REQUEST)

    delivery.successful = False
    delivery.save()

    user.driver_available = True
    user.save()
    mark_driver_available(user.id)

    # Create refund request for admin review (commission stays held)
    commission_txn = WalletTransaction.objects.filter(
        driver=user,
        delivery=delivery,
        transaction_type=WalletTransaction.COMMISSION,
    ).first()
    if commission_txn:
        CommissionRefundRequest.objects.create(
            driver=user,
            delivery=delivery,
            amount=abs(commission_txn.amount),
            reason=reason,
        )

    # Notify client
    client = delivery.client
    if client.fcm_token:
        try:
            msg = messaging.Message(
                token=client.fcm_token,
                data={"type": "delivery_cancelled", "delivery_id": str(delivery.delivery_id)},
                notification=messaging.Notification(
                    title="Delivery Cancelled",
                    body=f"Driver cancelled: {reason[:50]}",
                ),
            )
            messaging.send(msg)
        except Exception as e:
            logger.error(f"FCM driver cancel notification error: {e}")

    # B2B partner webhook
    partner_req = PartnerDeliveryRequest.objects.filter(delivery=delivery).first()
    if partner_req:
        partner_req.status = "cancelled"
        partner_req.save()
        from ..PartnerViews.webhooks import send_partner_webhook
        send_partner_webhook.send(str(partner_req.id), "cancelled", {"reason": "driver_cancelled"})

    return Response({"message": "Delivery cancelled. Refund request submitted for review."},
                    status=status.HTTP_200_OK)


@api_view(["GET"])
@permission_classes([IsAuthenticated])
def track_delivery(request):
    user = request.user
    delivery_id = request.query_params.get("delivery_id")

    if not delivery_id:
        return Response({"error": "delivery_id is required."}, status=status.HTTP_400_BAD_REQUEST)

    try:
        delivery = Delivery.objects.select_related('driver').get(delivery_id=delivery_id, client=user)
    except Delivery.DoesNotExist:
        return Response({"error": "Delivery not found."}, status=status.HTTP_404_NOT_FOUND)

    driver = delivery.driver
    # Determine if delivery is still in transit (no end_time and still successful)
    in_transit = delivery.end_time is None and delivery.successful

    # Get driver's live GPS location
    driver_lat = None
    driver_lng = None
    try:
        driver_loc = DriverLocations.objects.get(driver=driver)
        driver_lat = float(driver_loc.latitude) if driver_loc.latitude else None
        driver_lng = float(driver_loc.longitude) if driver_loc.longitude else None
    except DriverLocations.DoesNotExist:
        pass

    data = {
        "delivery_id": str(delivery.delivery_id),
        "origin_lat": delivery.origin_lat,
        "origin_lng": delivery.origin_lng,
        "destination_lat": delivery.destination_lat,
        "destination_lng": delivery.destination_lng,
        "vehicle": delivery.vehicle,
        "fare": float(delivery.fare),
        "payment_method": delivery.payment_method,
        "date": str(delivery.date),
        "in_transit": in_transit,
        "successful": delivery.successful,
        "picked_up": delivery.picked_up,
        "driver_name": f"{driver.name} {driver.surname}",
        "driver_rating": float(driver.rating) if driver.rating else 0.0,
        "driver_rating_count": driver.rating_count if hasattr(driver, 'rating_count') and driver.rating_count else 0,
        "driver_lat": driver_lat,
        "driver_lng": driver_lng,
    }

    return Response(data, status=status.HTTP_200_OK)


@api_view(["POST"])
@permission_classes([IsAuthenticated])
def rate_driver(request):
    delivery_id = request.data.get("delivery_id")
    rating_received = request.data.get("rating")

    delivery = get_object_or_404(Delivery, delivery_id=delivery_id)
    driver = delivery.driver
    driver_id = str(driver.id)

    if not delivery_id or not driver_id or rating_received is None:
        return Response({"error": "delivery_id and rating are required"},
                        status=status.HTTP_400_BAD_REQUEST)

    try:
        rating = Decimal(str(rating_received))
        if rating < Decimal('0.5') or rating > Decimal('5.0'):
            return Response({"error": "Rating must be between 0.5 and 5.0"}, status=status.HTTP_400_BAD_REQUEST)

        driver = CustomUser.objects.get(id=driver_id, role=CustomUser.DRIVER)

        # Update the driver's rating using weighted average
        if not hasattr(driver, 'rating_count') or driver.rating_count is None or driver.rating_count == 0:
            driver.rating_count = 1
            driver.rating = rating
        else:
            total_rating = driver.rating * driver.rating_count
            driver.rating_count += 1
            driver.rating = (total_rating + rating) / driver.rating_count
        driver.save()
        return Response({"message": "Rating submitted successfully"}, status=status.HTTP_200_OK)

    except Delivery.DoesNotExist:
        return Response({"error": "Delivery not found"}, status=status.HTTP_404_NOT_FOUND)

    except CustomUser.DoesNotExist:
        return Response({"error": "Driver not found"}, status=status.HTTP_404_NOT_FOUND)