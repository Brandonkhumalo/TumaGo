from rest_framework.decorators import api_view, permission_classes, throttle_classes
from rest_framework.permissions import IsAuthenticated
from rest_framework.response import Response
from rest_framework import status
from rest_framework.throttling import UserRateThrottle

class DeliveryRequestThrottle(UserRateThrottle):
    scope = 'delivery_request'
from .pagination import DeliveryCursorPagination
from django.db.models import Q
from django.shortcuts import get_object_or_404
from ...serializers.driverSerializer.authSerializers import UserSerializer
from ...serializers.driverSerializer.rideSerializers import DeliverySerializer
from ...models import DriverFinances, DriverVehicle, TripRequest, Delivery, CustomUser, Payment, DriverBalance, PartnerDeliveryRequest
from .deliveryMatching.delivery import driver_found
import googlemaps
import logging
from django.conf import settings

logger = logging.getLogger(__name__)
from TumaGo.firebase_init import initialize_firebase
from calendar import monthrange
from datetime import date, timedelta
from django.db.models import Sum
from decimal import Decimal
from .deliveryMatching.tasks import retry_trip_matching, publish_trip_accepted
from django.utils import timezone
initialize_firebase()

gmaps = googlemaps.Client(key=settings.GOOGLE_MAPS_API_KEY)

def convert_decimal(obj):
    if isinstance(obj, list):
        return [convert_decimal(i) for i in obj]
    elif isinstance(obj, dict):
        return {k: convert_decimal(v) for k, v in obj.items()}
    elif isinstance(obj, Decimal):
        return float(obj)  # or str(obj), depending on your preference
    else:
        return obj

@api_view(["POST"])
@permission_classes([IsAuthenticated])
def get_driver_data(request):
    user = request.user
    driver_data = UserSerializer(user)
    if DriverVehicle.objects.filter(driver=user).exists():

        driver_vehicles = DriverVehicle.objects.filter(driver=user)

        if driver_vehicles.count() > 1:
            # Keep the first one, delete the rest
            first_vehicle = driver_vehicles.first()
            driver_vehicles.exclude(id=first_vehicle.id).delete()
            logger.info("Deleted duplicate driver vehicles")
        
        return Response(driver_data.data, status=status.HTTP_200_OK)
    else:
        return Response(status=status.HTTP_404_NOT_FOUND)

@api_view(["GET"])
@permission_classes([IsAuthenticated])
def getDriver_Finances(request):
    user = request.user
    
    today = date.today()

    # Week: Sunday (start) to Saturday (end)
    sunday_start_offset = (today.weekday() + 1) % 7
    week_start = today - timedelta(days=sunday_start_offset)
    week_end = week_start + timedelta(days=6)

    # Month: 1st to last day (28/29/30/31 depending on month)
    month_start = today.replace(day=1)
    last_day = monthrange(today.year, today.month)[1]
    month_end = today.replace(day=last_day)

    # Shared aggregation fields — default=0 ensures None is never returned
    agg_fields = dict(
        earnings=Sum('earnings', default=Decimal('0.00')),
        charges=Sum('charges', default=Decimal('0.00')),
        profit=Sum('profit', default=Decimal('0.00')),
        total_trips=Sum('total_trips', default=0),
    )

    # Today (exact today match)
    today_data = DriverFinances.objects.filter(
        driver=user, today=today
    ).aggregate(**agg_fields)

    # This week: Sunday–Saturday using the week_start/week_end fields
    week_data = DriverFinances.objects.filter(
        driver=user,
        week_start__lte=week_end,
        week_end__gte=week_start,
    ).aggregate(**agg_fields)

    # This month: 1st–last day using the month_start/month_end fields
    month_data = DriverFinances.objects.filter(
        driver=user,
        month_start__lte=month_end,
        month_end__gte=month_start,
    ).aggregate(**agg_fields)

    # All time totals (no date filter)
    all_time = DriverFinances.objects.filter(driver=user).aggregate(**agg_fields)

    return Response({
        "today": today_data,
        "week": week_data,
        "month": month_data,
        "all_time": all_time,
    })

@api_view(["POST"])
@permission_classes([IsAuthenticated])
@throttle_classes([DeliveryRequestThrottle])
def RequestDelivery(request):
    delivery_serializer = DeliverySerializer(data=request.data)
    if delivery_serializer.is_valid():
        user = request.user
        delivery_data = delivery_serializer.validated_data
        user_id = user.id

        # Convert Decimal to float recursively before saving
        delivery_data_clean = convert_decimal(delivery_data)

        # Save Trip to DB
        trip = TripRequest.objects.create(
            requester=user,
            delivery_details=delivery_data_clean,
        )

        # Launch background countdown task with trip ID
        try:
            retry_trip_matching.send(str(trip.id), str(user_id), delivery_data_clean)
        except Exception as e:
            logger.error(f"Failed to start background task: {e}")

        return Response({"status": "Awaiting driver", "trip_id": str(trip.id)}, status=status.HTTP_200_OK)
    else:
        return Response(delivery_serializer.errors, status=status.HTTP_400_BAD_REQUEST)

@api_view(["POST"])
@permission_classes([IsAuthenticated])
def AcceptTrip(request):
    trip_id = request.data.get("trip_id")
    logger.info("Trip %s accepted", trip_id)
    driver_id = request.user.id
    Driver = get_object_or_404(CustomUser, id=driver_id)

    try:
        trip = TripRequest.objects.get(id=trip_id)
        trip.accepted = True
        trip.save()

        # Signal the background matching task to stop waiting (replaces sleep polling)
        publish_trip_accepted(str(trip_id))

        delivery_data = trip.delivery_details
        driver = request.user
        client = trip.requester

        origin_lng = delivery_data.get("origin_lng")
        origin_lat = delivery_data.get("origin_lat")
        destination_lat = delivery_data.get("destination_lat")
        destination_lng = delivery_data.get("destination_lng")
        vehicle = delivery_data.get("vehicle")
        fare = delivery_data.get("fare")
        payment_method = delivery_data.get("payment_method")

        delivery = Delivery.objects.create(
            driver=driver,
            client=client,
            start_time=timezone.now(),
            origin_lat=origin_lat,
            origin_lng=origin_lng,
            destination_lat=destination_lat,
            destination_lng=destination_lng,
            vehicle=vehicle,
            fare=fare,
            payment_method=payment_method
        )

        # ✅ Get driver's vehicle (use driver already fetched — avoids N+1)
        driver_vehicle = DriverVehicle.objects.select_related('driver').filter(driver=driver).first()
        if not driver_vehicle:
            logger.warning("Driver vehicle not found for driver %s", driver.id)
            return Response({'error': 'Driver vehicle not found'}, status=status.HTTP_404_NOT_FOUND)

        # ✅ Build delivery payload (driver details + vehicle + trip info)
        deliveryData = {
            "driver": f"{driver.name.capitalize()} {driver.surname.capitalize()}",
            "delivery_vehicle": driver_vehicle.delivery_vehicle,
            "vehicle_name": driver_vehicle.car_name,
            "number_plate": driver_vehicle.number_plate,
            "vehicle_model": driver_vehicle.vehicle_model,
            "color": driver_vehicle.color,
            "rating": driver.rating,
            "total_ratings": driver.rating_count,
            "fare": fare,
            "payment_method": payment_method,
            "vehicle_type": vehicle,
            "date": str(timezone.localtime(delivery.start_time)),
            "origin_lat": origin_lat,
            "origin_lng": origin_lng,
            "destination_lat": destination_lat,
            "destination_lng": destination_lng,
        }

        driver_found(client, deliveryData, str(delivery.delivery_id))
        logger.info("Delivery details sent to client")

        # B2B: If this trip is linked to a partner, update status + fire webhook
        partner_req = PartnerDeliveryRequest.objects.filter(trip_request=trip).first()
        if partner_req:
            partner_req.status = "driver_assigned"
            partner_req.delivery = delivery
            partner_req.save()
            from ..PartnerViews.webhooks import send_partner_webhook
            send_partner_webhook.send(str(partner_req.id), "driver_assigned")

        # Ensure the user is a driver before updating availability
        if Driver.role == CustomUser.DRIVER:
            Driver.driver_available = False
            Driver.save()
            return Response({'message': 'Driver availability set to false', 
                             'delivery_id': str(delivery.delivery_id)},
                             status=status.HTTP_202_ACCEPTED)
        else:
            return Response({'error': 'User is not a driver'}, status=status.HTTP_400_BAD_REQUEST)

    except TripRequest.DoesNotExist:
        logger.warning("Trip %s not found", trip_id)
        return Response({"error": "Trip not found"}, status=status.HTTP_404_NOT_FOUND)

    except Exception:
        logger.exception("Unhandled exception in AcceptTrip")
        return Response({"error": "An internal error occurred"}, status=status.HTTP_500_INTERNAL_SERVER_ERROR)
    
@api_view(["POST"])
@permission_classes([IsAuthenticated])
def end_trip(request):
    delivery_id = request.data.get("delivery_id")
    rating_received = request.data.get("rating")
    delivery_cost = request.data.get("delivery_cost")
    driver = request.user

    Driver_id = request.user.id
    Driver = get_object_or_404(CustomUser, id=Driver_id)

    delivery = get_object_or_404(Delivery, delivery_id=delivery_id)
    client = delivery.client
    client_id = str(client.id)

    if not delivery_id or not client_id or rating_received is None:
        return Response({"error": "delivery_id is required"},
                        {"error": "rating is required"},
                        status=status.HTTP_400_BAD_REQUEST)

    try:
        driver_vehicle = delivery.vehicle
        rating = Decimal(str(rating_received))
        if rating < Decimal('0.5') or rating > Decimal('5.0'):
            return Response({"error": "Rating must be between 0.5 and 5.0"}, status=status.HTTP_400_BAD_REQUEST)

        client = CustomUser.objects.get(id=client_id, role=CustomUser.USER)

        # Update the client's rating by adding
        if not hasattr(client, 'rating_count') or client.rating_count is None:
            client.rating_count = 1
            client.rating = rating_received
        else:
            total_rating = client.rating * client.rating_count
            client.rating_count += 1
            client.rating = (total_rating + rating_received) / client.rating_count
        client.save()
        
        # Update fields
        delivery.end_time = timezone.now()
        delivery.successful = True
        delivery.save()

        earnings = Decimal(delivery_cost)
        if driver_vehicle and driver_vehicle.lower() == "scooter":
            charges = Decimal("0.20")
        elif driver_vehicle and driver_vehicle.lower() == "van":
            charges = Decimal("0.30")
        elif driver_vehicle and driver_vehicle.lower() == "truck":
            charges = Decimal("0.50")
        else:
            charges = Decimal("0.10")

        finances = DriverFinances(earnings=earnings, charges=charges, driver=driver)
        finances.save()

        # If this delivery was paid online (card/ecocash/onemoney),
        # the money went to the platform — track what we owe the driver.
        payment_method = delivery.payment_method.lower()
        if payment_method in ('card', 'ecocash', 'onemoney'):
            driver_earnings = earnings - charges
            balance, _ = DriverBalance.objects.get_or_create(driver=driver)
            balance.owed_amount += driver_earnings
            balance.save()

            # Link the Payment record to this Delivery
            Payment.objects.filter(
                client=delivery.client,
                amount=delivery.fare,
                status=Payment.PAID,
                delivery__isnull=True,
            ).order_by('-created_at').update(delivery=delivery)

        # B2B: If this delivery is linked to a partner, update status + fire webhook
        partner_req = PartnerDeliveryRequest.objects.filter(delivery=delivery).first()
        if partner_req:
            partner_req.status = "delivered"
            partner_req.save()
            from ..PartnerViews.webhooks import send_partner_webhook
            send_partner_webhook.send(str(partner_req.id), "delivered")

            # Record financial split on the delivery charge transaction
            from ...models import PartnerTransaction
            partner = partner_req.partner
            commission = earnings * (partner.commission_rate / Decimal('100'))
            driver_payout = earnings - commission

            # Update the delivery_charge transaction with split details
            PartnerTransaction.objects.filter(
                delivery_request=partner_req,
                transaction_type='delivery_charge',
            ).update(
                system_commission=commission,
                driver_share=driver_payout,
                driver=driver,
                description=f"Delivery completed by {driver.name} {driver.surname}",
            )

        if Driver.role == CustomUser.DRIVER:
            Driver.driver_available = True
            Driver.save()
            return Response({"message": "Trip ended successfully"}, status=status.HTTP_200_OK)
        else:
            return Response({'error': 'User is not a driver'}, status=status.HTTP_400_BAD_REQUEST)

    except Delivery.DoesNotExist:
        return Response({"error": "Delivery not found"}, status=status.HTTP_404_NOT_FOUND)
    
    except CustomUser.DoesNotExist:
        return Response({"error": "Client not found"}, status=status.HTTP_404_NOT_FOUND)
    
@api_view(['GET'])
@permission_classes([IsAuthenticated])
def get_deliveries(request):
    user = request.user

    # Get successful deliveries related to the user (as client or driver)
    deliveries = Delivery.objects.select_related('driver', 'client').filter(
    Q(client=user) | Q(driver=user), successful=True).order_by('-start_time')

    # Paginate
    paginator = DeliveryCursorPagination()
    result_page = paginator.paginate_queryset(deliveries, request)
    serializer = DeliverySerializer(result_page, many=True)
    logger.debug("Delivery history query returned %d results", len(serializer.data))
    return paginator.get_paginated_response(serializer.data)