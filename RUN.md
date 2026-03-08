# Running TumaGo Backends

## Docker (Recommended)

Runs all services including PostgreSQL, PgBouncer, and Redis.

```bash
# Development
cd backend
docker compose up -d --build

# Production
cd backend
docker compose -f docker-compose.prod.yml up -d --build
```

## Individual Services (Manual)

> **Prerequisites:** PostgreSQL 15, PgBouncer, and Redis 7 must be running.
> Go 1.22+ is required for Go services (not installed locally — use Docker instead).

### Django (port 8000)

```bash
cd backend/django
python manage.py migrate
python manage.py runserver
# Or with ASGI:
daphne -b 0.0.0.0 -p 8000 TumaGo.asgi:application
```

Background matching workers:

```bash
dramatiq TumaGo_Server.views.DriverViews.deliveryMatching.tasks --processes 2 --threads 4
```

### Go Gateway (port 80)

```bash
cd backend/go_services/gateway
go build -o gateway . && ./gateway
```

### Go Location Service (port 8001)

```bash
cd backend/go_services/location
go build -o location-service . && ./location-service
```

### Go Matching Service (port 8002)

```bash
cd backend/go_services/matching
go build -o matching-service . && ./matching-service
```

### Notification Service (port 8003)

```bash
cd backend/notification
pip install -r requirements.txt
uvicorn main:app --host 0.0.0.0 --port 8003
```

## Environment Variables

### Go Services

`SECRET_KEY`, `REDIS_URL`, `DATABASE_URL`, `LISTEN_ADDR`

Gateway also needs: `DJANGO_BACKEND`, `LOCATION_SERVICE`, `MATCHING_SERVICE`, `STATIC_DIR`

### Django

Configured via `decouple.config()`: `SECRET_KEY`, `DATABASE_URL`, `REDIS_URL`, `FIREBASE_CREDENTIALS`, `GOOGLE_MAPS_API_KEY`

See [backend/django/TumaGo/settings.py](backend/django/TumaGo/settings.py) for full list.
