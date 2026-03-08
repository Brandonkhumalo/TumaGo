# TumaGo — Claude Context

## What Is This

TumaGo is a last-mile package delivery platform. Senders request deliveries via an Android app; drivers accept and complete them via a separate Android driver app. A hybrid Go + Django backend coordinates matching, real-time tracking, and notifications.

---

## Tech Stack

| Layer | Technology |
|-------|-----------|
| API Gateway | Go 1.22 (stdlib + x/time/rate) — routing, rate limiting, JWT |
| Location Service | Go 1.22 + gorilla/websocket + go-redis + pgx — WebSocket GPS |
| Matching Service | Go 1.22 + go-redis + pgx — Redis GEOSEARCH + DB filtering |
| Backend API | Django 5.2 + Django REST Framework 3.16 — auth, CRUD, admin |
| Async tasks | Dramatiq 1.18 + Redis |
| Notifications | FastAPI + Firebase Admin SDK 6.8 (FCM) |
| Database | PostgreSQL 15 + PgBouncer (connection pooling) |
| Cache/Queue/GEO | Redis 7 (Channels, Dramatiq broker, cache, GEO driver locations) |
| Auth | Custom JWT (HS256) via `token.py` — shared secret with Go services |
| Distance calculation | Google Maps API |
| Media storage | Google Cloud Storage |
| Client app | Android (Java), minSdk 24, compileSdk 35 |
| Driver app | Android (Java), same SDK targets + Java-WebSocket 1.6 |
| HTTP client (Android) | Retrofit 2.9 + Gson + OkHttp RetryInterceptor |
| Android security | AndroidX Security EncryptedSharedPreferences |

---

## Architecture

```
Client → Go Gateway (:80) → Django (:8000)  — auth, CRUD, admin
                           → Go Location (:8001) — WebSocket GPS streaming
                           → Go Matching (:8002)  — driver matching (internal only)
                           → notification (:8003) — FCM push (Python)
```

---

## Project Structure

```
TumaGo/
├── backend/                     # All backend code
│   ├── django/                  # Django project
│   │   ├── TumaGo/             # Settings, ASGI, URLs, broker, Firebase init
│   │   ├── TumaGo_Server/      # Single Django app
│   │   │   ├── models.py       # All 7 database models
│   │   │   ├── token.py        # Custom JWT (shared HS256 secret with Go)
│   │   │   ├── serializers/    # Input validation
│   │   │   ├── views/          # UserViews/ + DriverViews/
│   │   │   └── location_tracking/  # Django Channels WS consumer (fallback)
│   │   ├── Dockerfile
│   │   └── requirements.txt
│   ├── go_services/
│   │   ├── gateway/            # Go API gateway (main.go, proxy.go, ratelimit.go, auth.go)
│   │   ├── location/           # Go WebSocket service (main.go, ws.go, auth.go, db.go)
│   │   └── matching/           # Go matching service (main.go, handler.go, redis.go, db.go)
│   ├── notification/           # Python FastAPI — FCM push notifications
│   ├── infrastructure/         # K8s manifests, nginx, monitoring
│   ├── docker-compose.yml      # Development compose
│   └── docker-compose.prod.yml # Production compose with replicas
├── TumaGo_client/              # Android app for senders
└── TumaGo_driver/              # Android app for drivers
```

Key files:
- [backend/go_services/gateway/main.go](backend/go_services/gateway/main.go) — API gateway
- [backend/go_services/location/ws.go](backend/go_services/location/ws.go) — WebSocket handler
- [backend/go_services/matching/handler.go](backend/go_services/matching/handler.go) — /match endpoint
- [backend/django/TumaGo_Server/models.py](backend/django/TumaGo_Server/models.py) — all 7 DB models
- [backend/django/TumaGo_Server/token.py](backend/django/TumaGo_Server/token.py) — JWT auth
- [backend/django/TumaGo/settings.py](backend/django/TumaGo/settings.py) — environment config
- [backend/django/TumaGo_Server/urls.py](backend/django/TumaGo_Server/urls.py) — 28 API endpoints

---

## Build & Run Commands

### Docker (all services)

```bash
# Development (from backend/)
cd backend && docker compose up -d --build

# Production (from backend/)
cd backend && docker compose -f docker-compose.prod.yml up -d --build
```

### Django only (from backend/django/)

```bash
python manage.py runserver
python manage.py migrate
python manage.py makemigrations
daphne -b 0.0.0.0 -p 8000 TumaGo.asgi:application
dramatiq TumaGo_Server.views.DriverViews.deliveryMatching.tasks --processes 2 --threads 4
```

### Go services (requires Go 1.22+)

```bash
cd backend/go_services/gateway && go build -o gateway . && ./gateway
cd backend/go_services/location && go build -o location-service . && ./location-service
cd backend/go_services/matching && go build -o matching-service . && ./matching-service
```

### Android Apps

```bash
cd TumaGo_client && ./gradlew assembleDebug
cd TumaGo_driver && ./gradlew assembleDebug
```

---

## Environment Variables

### Go services
`SECRET_KEY` (shared JWT secret), `REDIS_URL`, `DATABASE_URL`, `LISTEN_ADDR`
Gateway also needs: `DJANGO_BACKEND`, `LOCATION_SERVICE`, `MATCHING_SERVICE`, `STATIC_DIR`

### Django
All secrets via `decouple.config()`: `SECRET_KEY`, `DATABASE_URL`, `REDIS_URL`, `FIREBASE_CREDENTIALS`, `GOOGLE_MAPS_API_KEY`. See [settings.py](backend/django/TumaGo/settings.py).

### Android
`BASE_URL` hardcoded in `ApiClient.java` — update when switching environments.
