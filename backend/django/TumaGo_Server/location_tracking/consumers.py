import json
from channels.generic.websocket import AsyncWebsocketConsumer
from channels.db import database_sync_to_async
import redis.asyncio as aioredis
from django.conf import settings

# Module-level async Redis client (one connection pool shared per process)
_redis_client = None

async def get_redis():
    global _redis_client
    if _redis_client is None:
        _redis_client = await aioredis.from_url(
            settings.REDIS_URL,
            encoding="utf-8",
            decode_responses=True,
        )
    return _redis_client


class LocationConsumer(AsyncWebsocketConsumer):
    """
    WebSocket consumer that receives driver GPS coordinates and:
      1. Stores them in Redis GEO (O(log N), instant geospatial queries)
      2. Upserts the DriverLocations DB row (one row per driver, not one per message)

    Redis GEO key: "driver_locations"
    Member name:   str(driver.id)  — UUID
    """

    async def connect(self):
        self.driver_id = None
        self.scope['user'] = await self.get_user_from_token()

        if self.scope['user'] and self.scope['user'].is_authenticated:
            self.driver_id = str(self.scope['user'].id)
            await self.accept()
        else:
            await self.close()

    async def disconnect(self, close_code):
        # Remove driver from the live Redis GEO index on disconnect
        if self.driver_id:
            try:
                r = await get_redis()
                await r.zrem("driver_locations", self.driver_id)
            except Exception as e:
                print(f"Redis cleanup error on disconnect: {e}")

    async def receive(self, text_data):
        try:
            data = json.loads(text_data)
            latitude = data.get("latitude")
            longitude = data.get("longitude")
            user = self.scope['user']

            if user and latitude is not None and longitude is not None:
                # 1. Update Redis GEO — O(log N), used by matching service
                await self.update_redis_geo(str(user.id), float(longitude), float(latitude))
                # 2. Upsert DB row — one row per driver, not one per message
                await self.upsert_location(user, latitude, longitude)
        except (json.JSONDecodeError, TypeError, ValueError) as e:
            print(f"Error processing WebSocket message: {e}")

    async def update_redis_geo(self, driver_id: str, longitude: float, latitude: float):
        """Store/update driver position in Redis GEO set."""
        try:
            r = await get_redis()
            # GEOADD overwrites existing entry for the same member
            await r.geoadd("driver_locations", [longitude, latitude, driver_id])
        except Exception as e:
            print(f"Redis GEO update error for driver {driver_id}: {e}")

    @database_sync_to_async
    def upsert_location(self, user, lat, lng):
        """Upsert — one DB row per driver, updated in place instead of appended."""
        from ..models import DriverLocations
        DriverLocations.objects.update_or_create(
            driver=user,
            defaults={"latitude": str(lat), "longitude": str(lng)},
        )

    @database_sync_to_async
    def get_user_from_token(self):
        from urllib.parse import parse_qs
        from django.contrib.auth.models import AnonymousUser
        from jwt import decode, InvalidTokenError, ExpiredSignatureError
        from django.conf import settings
        from django.contrib.auth import get_user_model
        from datetime import datetime

        User = get_user_model()

        try:
            query_string = self.scope['query_string'].decode()
            query_params = parse_qs(query_string)
            token = query_params.get("token", [None])[0]

            if not token:
                return AnonymousUser()

            payload = decode(token, key=settings.SECRET_KEY, algorithms=["HS256"])

            if "exp" not in payload or int(datetime.utcnow().timestamp()) > payload["exp"]:
                raise ExpiredSignatureError("Token expired")

            if payload.get("type") != "access_token":
                raise InvalidTokenError("Invalid token type")

            user_id = payload.get("id")
            if not user_id:
                return AnonymousUser()

            return User.objects.get(id=user_id)

        except (InvalidTokenError, ExpiredSignatureError, User.DoesNotExist, Exception) as e:
            print(f"WebSocket Token Error: {e}")
            return AnonymousUser()
