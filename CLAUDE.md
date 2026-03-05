# TumaGo — Claude Context

## What Is This

TumaGo is a last-mile package delivery platform. Senders request deliveries via an Android app; drivers accept and complete them via a separate Android driver app. The backend coordinates matching, real-time tracking, and notifications.

---

## Tech Stack

| Layer | Technology |
|-------|-----------|
| Backend API | Django 5.2 + Django REST Framework 3.16 |
| WebSockets | Django Channels 4.2 + Daphne |
| Async tasks | Dramatiq 1.18 + Redis |
| Database | PostgreSQL (production), MySQL supported |
| Auth | Custom JWT (HS256) via `token.py` — not SimpleJWT views |
| Push notifications | Firebase Admin SDK 6.8 (FCM) |
| Distance calculation | Google Maps API |
| Media storage | Google Cloud Storage |
| Client app | Android (Java), minSdk 24, compileSdk 35 |
| Driver app | Android (Java), same SDK targets + Java-WebSocket 1.6 |
| HTTP client (Android) | Retrofit 2.9 + Gson |
| Android security | AndroidX Security EncryptedSharedPreferences |

---

## Project Structure

```
TumaGo/
├── TumaGo_Backend/          # Django project
│   ├── TumaGo/              # Django settings, ASGI, URLs, broker, Firebase init
│   └── TumaGo_Server/       # Single Django app
│       ├── models.py        # All database models
│       ├── token.py         # Custom JWT implementation
│       ├── serializers/     # Input validation (3 files, 10 serializers)
│       ├── views/
│       │   ├── UserViews/   # Client-facing endpoints
│       │   └── DriverViews/ # Driver endpoints + delivery matching
│       └── location_tracking/ # WebSocket consumer
├── TumaGo_client/           # Android app for senders (47 Java files)
│   └── app/src/main/java/com/techmania/tumago/
│       ├── Activities/      # Main UI screens
│       ├── auth/            # Login, register, splash, verification
│       ├── helper/          # ApiClient, Token, FCM, sync utilities
│       ├── Interface/       # Retrofit ApiService interface
│       ├── Model/           # Request/response POJOs
│       └── adapter/         # RecyclerView adapters
└── TumaGo_driver/           # Android app for drivers (46 Java files)
    └── app/src/main/java/com/techmania/tumago_driver/
        └── (mirrors client structure with driver-specific screens)
```

Key files:
- [TumaGo_Backend/TumaGo_Server/models.py](TumaGo_Backend/TumaGo_Server/models.py) — all 7 DB models
- [TumaGo_Backend/TumaGo_Server/token.py](TumaGo_Backend/TumaGo_Server/token.py) — JWT auth
- [TumaGo_Backend/TumaGo/settings.py](TumaGo_Backend/TumaGo/settings.py) — environment config
- [TumaGo_Backend/TumaGo/asgi.py](TumaGo_Backend/TumaGo/asgi.py) — HTTP + WebSocket routing
- [TumaGo_Backend/TumaGo_Server/urls.py](TumaGo_Backend/TumaGo_Server/urls.py) — 28 API endpoints

---

## Build & Run Commands

### Backend

```bash
# Development
python manage.py runserver

# Production (ASGI + WebSockets)
daphne -b 0.0.0.0 -p 8000 TumaGo.asgi:application

# Database
python manage.py migrate
python manage.py makemigrations

# Async worker (required for delivery matching)
dramatiq TumaGo_Server.views.DriverViews.deliveryMatching.tasks --processes 2 --threads 4

# Docker (all services: web, redis, worker, db)
docker-compose up
```

### Android Apps (run from `TumaGo_client/` or `TumaGo_driver/`)

```bash
./gradlew assembleDebug
./gradlew installDebug
./gradlew test
./gradlew connectedAndroidTest
```

---

## Environment Variables

All secrets are read via `decouple.config()`. Required vars: `SECRET_KEY`, `DATABASE_URL`, `REDIS_URL`, Firebase credentials, `GOOGLE_MAPS_API_KEY`, email SMTP config. See [TumaGo_Backend/TumaGo/settings.py](TumaGo_Backend/TumaGo/settings.py) for the full list.

The Android `BASE_URL` in `ApiClient.java` is hardcoded — update it when switching environments.

---

## Additional Documentation

Check these files when working on the relevant area:

| Topic | File |
|-------|------|
| Architectural patterns & design decisions | [.claude/docs/architectural_patterns.md](.claude/docs/architectural_patterns.md) |
