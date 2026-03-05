# Architectural Patterns

Patterns confirmed across multiple files in TumaGo.

---

## 1. Authentication — Custom JWT (Backend)

**Files**: [TumaGo_Backend/TumaGo_Server/token.py](../../TumaGo_Backend/TumaGo_Server/token.py), [TumaGo_Backend/TumaGo_Server/views/UserViews/views.py](../../TumaGo_Backend/TumaGo_Server/views/UserViews/views.py), [TumaGo_Backend/TumaGo_Server/location_tracking/consumers.py](../../TumaGo_Backend/TumaGo_Server/location_tracking/consumers.py)

`JWTAuthentication(BaseAuthentication)` is used instead of DRF SimpleJWT for all views. Token lifetimes: 30-day access, 60-day refresh. On logout, tokens are added to `BlacklistedToken` — checked on every `authenticate()` call. WebSocket connections also authenticate via JWT passed as a query parameter.

- Token generation: [token.py:20-37](../../TumaGo_Backend/TumaGo_Server/token.py#L20-L37)
- Blacklist check: [token.py:58-86](../../TumaGo_Backend/TumaGo_Server/token.py#L58-L86)
- WebSocket auth: [consumers.py:1-76](../../TumaGo_Backend/TumaGo_Server/location_tracking/consumers.py#L1-L76)

---

## 2. API View Pattern — FBV + Decorator Composition (Backend)

**Files**: [views/UserViews/views.py](../../TumaGo_Backend/TumaGo_Server/views/UserViews/views.py), [views/DriverViews/authViews.py](../../TumaGo_Backend/TumaGo_Server/views/DriverViews/authViews.py), [views/DriverViews/driverViews.py](../../TumaGo_Backend/TumaGo_Server/views/DriverViews/driverViews.py)

All 28 endpoints use `@api_view` + `@permission_classes` decorators — no class-based views. Public endpoints use `AllowAny`; protected endpoints use `IsAuthenticated` with the custom JWT class. The flow is always: deserialize → validate → act → return `Response({}, status=...)`.

- Public endpoint example: [views.py:24-40](../../TumaGo_Backend/TumaGo_Server/views/UserViews/views.py#L24-L40)
- Protected endpoint example: [authViews.py:40-54](../../TumaGo_Backend/TumaGo_Server/views/DriverViews/authViews.py#L40-L54)

---

## 3. Serializer-First Validation (Backend)

**Files**: [serializers/userSerializers.py](../../TumaGo_Backend/TumaGo_Server/serializers/userSerializers.py), [serializers/driverSerializers.py](../../TumaGo_Backend/TumaGo_Server/serializers/driverSerializers.py), [serializers/rideSerializers.py](../../TumaGo_Backend/TumaGo_Server/serializers/rideSerializers.py)

Views never validate request data directly. All input passes through a serializer (`serializer.is_valid(raise_exception=True)` pattern), then `serializer.save()` or manual model creation. 10 serializers across 3 files cover every input surface.

---

## 4. Singleton Retrofit Client (Android)

**Files**: [TumaGo_client/.../helper/ApiClient.java](../../TumaGo_client/app/src/main/java/com/techmania/tumago/helper/ApiClient.java), [TumaGo_driver/.../helpers/ApiClient.java](../../TumaGo_driver/app/src/main/java/com/techmania/tumago_driver/helpers/ApiClient.java)

Both apps use an identical singleton pattern: a static `Retrofit` field initialized once via `getClient()`. The base URL (`BASE_URL`) is hardcoded — swap it per environment. A second `ApiClient2` exists in the driver app for OkHttp-specific requests.

- Client: [ApiClient.java:1-19](../../TumaGo_client/app/src/main/java/com/techmania/tumago/helper/ApiClient.java#L1-L19)
- Driver: [ApiClient.java:1-19](../../TumaGo_driver/app/src/main/java/com/techmania/tumago_driver/helpers/ApiClient.java#L1-L19)

---

## 5. Retrofit Interface + Callback Pattern (Android)

**Files**: [TumaGo_client/.../Interface/ApiService.java](../../TumaGo_client/app/src/main/java/com/techmania/tumago/Interface/ApiService.java), [TumaGo_driver/.../Interface/ApiService.java](../../TumaGo_driver/app/src/main/java/com/techmania/tumago_driver/Interface/ApiService.java)

All HTTP calls are declared in a single `ApiService` interface per app. Auth header is passed explicitly on every call as `@Header("Authorization") String authHeader` (format: `"Bearer <token>"`). Pagination uses `@Query("cursor")`. Activities consume the API via anonymous `Callback<T>` implementations inline.

- Client interface: [ApiService.java:1-85](../../TumaGo_client/app/src/main/java/com/techmania/tumago/Interface/ApiService.java#L1-L85)

---

## 6. Encrypted Token Storage (Android)

**Files**: [TumaGo_client/.../helper/Token.java](../../TumaGo_client/app/src/main/java/com/techmania/tumago/helper/Token.java), [TumaGo_driver/.../auth/Token.java](../../TumaGo_driver/app/src/main/java/com/techmania/tumago_driver/auth/Token.java)

Both apps store JWT tokens using AndroidX Security `EncryptedSharedPreferences` with `AES256_GCM` value encryption and `AES256_SIV` key encryption. The same `Token` class handles store, get, and clear operations. Delivery IDs are also persisted here.

- Client: [Token.java:1-122](../../TumaGo_client/app/src/main/java/com/techmania/tumago/helper/Token.java#L1-L122)

---

## 7. FCM Push → Activity Trigger Pattern (Android)

**Files**: [TumaGo_client/.../helper/ClientMessagingService.java](../../TumaGo_client/app/src/main/java/com/techmania/tumago/helper/ClientMessagingService.java), [TumaGo_client/.../helper/DriverFoundHelper.java](../../TumaGo_client/app/src/main/java/com/techmania/tumago/helper/DriverFoundHelper.java), [TumaGo_client/.../helper/NoDriverFoundHelper.java](../../TumaGo_client/app/src/main/java/com/techmania/tumago/helper/NoDriverFoundHelper.java)

Firebase Cloud Messaging is the primary real-time update mechanism for the client app. `onMessageReceived` in the messaging service delegates to purpose-specific helper classes (`DriverFoundHelper`, `NoDriverFoundHelper`) that launch the appropriate `Activity` via an `Intent`. The driver app uses a parallel structure.

---

## 8. Async Delivery Matching with Retry (Backend)

**Files**: [deliveryMatching/tasks.py](../../TumaGo_Backend/TumaGo_Server/views/DriverViews/deliveryMatching/tasks.py), [deliveryMatching/delivery.py](../../TumaGo_Backend/TumaGo_Server/views/DriverViews/deliveryMatching/delivery.py), [dramatiq_broker.py](../../TumaGo_Backend/TumaGo/dramatiq_broker.py)

Trip matching is offloaded to a Dramatiq actor (`@dramatiq.actor(max_retries=4)`). The algorithm: query available drivers → filter by vehicle type → compute distance via Google Maps API → pick closest → send FCM notification. If the driver doesn't accept within the countdown window, the actor retries up to 4 times, then sends a `no_driver_found` FCM notification to the client.

- Retry actor: [tasks.py:1-84](../../TumaGo_Backend/TumaGo_Server/views/DriverViews/deliveryMatching/tasks.py#L1-L84)
- Matching logic: [delivery.py:10-86](../../TumaGo_Backend/TumaGo_Server/views/DriverViews/deliveryMatching/delivery.py#L10-L86)

---

## 9. Role-Based User Model (Backend)

**Files**: [TumaGo_Backend/TumaGo_Server/models.py](../../TumaGo_Backend/TumaGo_Server/models.py)

A single `CustomUser(AbstractUser)` model serves both roles with a `role` field (`USER` | `DRIVER`). UUID primary keys throughout. Driver-specific state (`driver_online`, `driver_available`) lives on the same model. Related models (`DriverLocations`, `DriverFinances`, `DriverVehicle`) are linked via `OneToOneField` or `ForeignKey`.

- Model definition: [models.py:25-88](../../TumaGo_Backend/TumaGo_Server/models.py#L25-L88)

---

## 10. Environment-Driven Configuration (Backend)

**Files**: [TumaGo_Backend/TumaGo/settings.py](../../TumaGo_Backend/TumaGo/settings.py), [firebase_init.py](../../TumaGo_Backend/TumaGo/firebase_init.py), [dramatiq_broker.py](../../TumaGo_Backend/TumaGo/dramatiq_broker.py), [docker-compose.yml](../../TumaGo_Backend/docker-compose.yml)

All secrets and infrastructure URLs are read from environment variables via `decouple.config()`. No secrets are hardcoded in settings. Firebase credentials, `DATABASE_URL`, `REDIS_URL`, `SECRET_KEY`, Google Maps key, and email credentials all follow this pattern.

- Settings: [settings.py:1-151](../../TumaGo_Backend/TumaGo/settings.py#L1-L151)
- Firebase init: [firebase_init.py:1-19](../../TumaGo_Backend/TumaGo/firebase_init.py#L1-L19)
