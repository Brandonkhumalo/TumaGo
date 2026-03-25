"""
Driver matching engine.

Old approach: loop every driver → call Google Maps per driver → O(N) external API calls.
New approach:
  1. GEOSEARCH Redis GEO set "driver_locations" for drivers within radius.
  2. Filter by vehicle type and availability in a single DB query.
  3. Call Google Maps ONCE for the selected driver (ETA only — optional).
"""
from ....models import DriverLocations, DriverVehicle, CustomUser
from firebase_admin import messaging
from TumaGo.firebase_init import initialize_firebase
import googlemaps
import redis
from django.conf import settings

import logging
logger = logging.getLogger(__name__)

gmaps = googlemaps.Client(key=settings.GOOGLE_MAPS_API_KEY)
initialize_firebase()

# Synchronous Redis client (Dramatiq workers run in threads, not async)
_sync_redis = None


def get_sync_redis():
    global _sync_redis
    if _sync_redis is None:
        _sync_redis = redis.Redis.from_url(settings.REDIS_URL, decode_responses=True)
    return _sync_redis


def find_nearest_driver(requester_lat, requester_lng, requested_vehicle_type, search_radius_km=15):
    """
    Query Redis GEO for nearby drivers, then filter by vehicle + availability
    in a single DB query.  Returns (driver, coords_dict) or (None, None).
    """
    r = get_sync_redis()

    try:
        nearby = r.geosearch(
            "driver_locations",
            longitude=float(requester_lng),
            latitude=float(requester_lat),
            radius=search_radius_km,
            unit="km",
            sort="ASC",
            withcoord=True,
            withdist=True,
        )
    except redis.RedisError as e:
        logger.error(f"Redis GEOSEARCH error: {e}")
        return _db_fallback_find(requester_lat, requester_lng, requested_vehicle_type)

    if not nearby:
        return None, None

    driver_ids_ordered = [entry[0] for entry in nearby]
    coords_map = {
        entry[0]: {"latitude": entry[2][1], "longitude": entry[2][0]}
        for entry in nearby
    }

    # Single DB query — available drivers with matching vehicle type
    drivers_qs = CustomUser.objects.filter(
        id__in=driver_ids_ordered,
        driver_available=True,
        role=CustomUser.DRIVER,
        driver_vehicles__delivery_vehicle=requested_vehicle_type,
    )
    drivers_map = {str(d.id): d for d in drivers_qs}

    # Return the closest valid driver (Redis results are already distance-sorted)
    for driver_id in driver_ids_ordered:
        driver = drivers_map.get(driver_id)
        if driver:
            return driver, coords_map[driver_id]

    return None, None


def _db_fallback_find(requester_lat, requester_lng, requested_vehicle_type):
    """Fallback when Redis is unavailable — simple Euclidean scan of DriverLocations."""
    logger.warning("Using DB fallback for driver matching (Redis unavailable)")
    driver_locations = DriverLocations.objects.select_related('driver').filter(
        driver__driver_available=True,
        driver__role=CustomUser.DRIVER,
    )

    best_driver = None
    best_coords = None
    best_distance = float('inf')

    for loc in driver_locations:
        driver = loc.driver
        try:
            DriverVehicle.objects.get(driver=driver, delivery_vehicle=requested_vehicle_type)
        except DriverVehicle.DoesNotExist:
            continue

        if not loc.latitude or not loc.longitude:
            continue

        dlat = float(loc.latitude) - float(requester_lat)
        dlng = float(loc.longitude) - float(requester_lng)
        dist = (dlat ** 2 + dlng ** 2) ** 0.5

        if dist < best_distance:
            best_distance = dist
            best_driver = driver
            best_coords = {"latitude": float(loc.latitude), "longitude": float(loc.longitude)}

    return best_driver, best_coords


def TripData(requester_data, delivery_data, trip_id):
    destination_lat = delivery_data['destination_lat']
    destination_lng = delivery_data['destination_lng']
    requester_lng = delivery_data['origin_lng']
    requester_lat = delivery_data['origin_lat']
    requested_vehicle_type = delivery_data['vehicle']
    cost = delivery_data['fare']

    requester_name = f"{requester_data['name']} {requester_data['surname']}"

    closest_driver, closest_driver_coords = find_nearest_driver(
        requester_lat, requester_lng, requested_vehicle_type
    )

    if closest_driver:
        # Delivery distance (pickup → destination) — shown to driver alongside fare
        distance_meters = _get_distance_meters(
            requester_lat, requester_lng,
            destination_lat, destination_lng,
        )

        payload = {
            "driver_name": f"{closest_driver.name} {closest_driver.surname}",
            "email": closest_driver.email,
            "coordinates": closest_driver_coords,
            "distance_meters": distance_meters,
            "requester_Name": requester_name,
            "destination_lat": destination_lat,
            "destination_lng": destination_lng,
            "requester_lng": requester_lng,
            "requester_lat": requester_lat,
            "cost": cost,
        }

        logger.info(f"Driver matched: {closest_driver.name} ({closest_driver.id})")
        send_request_to_driver(closest_driver, payload, trip_id)
        return payload

    logger.info("No valid drivers found in radius for trip %s", trip_id)
    return None


def _get_distance_meters(origin_lat, origin_lng, dest_lat, dest_lng):
    """One Google Maps call after matching — for accurate driving distance.

    Results are cached in Redis for 24 hours keyed by rounded lat/lng (4 decimal
    places ≈ 11m accuracy) so repeat deliveries on similar routes skip the API.
    """
    # Round to 4 decimal places — same street-level accuracy, far fewer cache misses
    o_lat = round(float(origin_lat), 4)
    o_lng = round(float(origin_lng), 4)
    d_lat = round(float(dest_lat), 4)
    d_lng = round(float(dest_lng), 4)

    cache_key = f"gmaps:dist:{o_lat},{o_lng}:{d_lat},{d_lng}"

    # Check Redis cache first
    try:
        r = get_sync_redis()
        cached = r.get(cache_key)
        if cached is not None:
            logger.debug("Google Maps cache hit: %s → %s meters", cache_key, cached)
            return int(cached)
    except Exception as e:
        logger.warning("Redis cache read failed (proceeding to API): %s", e)

    # Cache miss — call Google Maps
    try:
        result = gmaps.distance_matrix(
            origins=[f"{o_lat},{o_lng}"],
            destinations=[f"{d_lat},{d_lng}"],
            mode='driving',
        )
        element = result['rows'][0]['elements'][0]
        if element['status'] == 'OK':
            meters = element['distance']['value']
            # Cache for 24 hours
            try:
                r = get_sync_redis()
                r.set(cache_key, meters, ex=86400)
            except Exception as e:
                logger.warning("Redis cache write failed: %s", e)
            return meters
    except Exception as e:
        logger.error("Google Maps distance error: %s", e)
    return 0


def send_request_to_driver(driver, request_payload, trip_id):
    if not driver.fcm_token:
        return False

    message = messaging.Message(
        token=driver.fcm_token,
        data={
            "type": "new_request",
            "requester_name": request_payload["requester_Name"],
            "destination_lat": str(request_payload["destination_lat"]),
            "destination_lng": str(request_payload["destination_lng"]),
            "requester_lng": str(request_payload["requester_lng"]),
            "requester_lat": str(request_payload["requester_lat"]),
            "distance_meters": str(request_payload["distance_meters"]),
            "cost": str(request_payload["cost"]),
            "trip_id": str(trip_id),
        },
        notification=messaging.Notification(
            title="New Delivery Request",
            body=f"{request_payload['requester_Name']} needs a delivery nearby!",
        ),
    )

    try:
        response = messaging.send(message)
        logger.info(f"FCM sent to driver {driver.id}: {response}")
        return True
    except Exception as e:
        logger.error(f"FCM send error for driver {driver.id}: {e}")
        return False


def driver_found(user, delivery_payload, delivery_id):
    if not user.fcm_token:
        return

    message = messaging.Message(
        token=user.fcm_token,
        data={
            "type": "driver_found",
            "driver_name": delivery_payload.get("driver", ""),
            "vehicle": str(delivery_payload["delivery_vehicle"]),
            "vehicle_name": str(delivery_payload["vehicle_name"]),
            "number_plate": str(delivery_payload["number_plate"]),
            "vehicle_model": str(delivery_payload["vehicle_model"]),
            "color": str(delivery_payload["color"]),
            "delivery_id": str(delivery_id),
            "rating": str(delivery_payload["rating"]),
            "total_ratings": str(delivery_payload["total_ratings"]),
            "fare": str(delivery_payload.get("fare", "")),
            "payment_method": str(delivery_payload.get("payment_method", "")),
            "vehicle_type": str(delivery_payload.get("vehicle_type", "")),
            "date": str(delivery_payload.get("date", "")),
            "origin_lat": str(delivery_payload.get("origin_lat", "")),
            "origin_lng": str(delivery_payload.get("origin_lng", "")),
            "destination_lat": str(delivery_payload.get("destination_lat", "")),
            "destination_lng": str(delivery_payload.get("destination_lng", "")),
        },
        notification=messaging.Notification(
            title="Driver Found",
            body="A driver has accepted your request!",
        ),
    )

    try:
        response = messaging.send(message)
        logger.info(f"Driver found notification sent: {response}")
    except Exception as e:
        logger.error(f"FCM driver_found error: {e}")


def no_driver_found(user):
    if not user.fcm_token:
        return

    message = messaging.Message(
        notification=messaging.Notification(
            title="No Drivers Found",
            body="We couldn't find an available driver. Please try again shortly.",
        ),
        token=user.fcm_token,
        data={"type": "no_driver_found"},
    )

    try:
        response = messaging.send(message)
        logger.info(f"No-driver notification sent: {response}")
    except Exception as e:
        logger.error(f"FCM no_driver_found error: {e}")
