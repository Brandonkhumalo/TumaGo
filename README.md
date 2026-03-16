# TumaGo — Last-Mile Package Delivery Platform

TumaGo connects clients who need to send packages with nearby drivers based on vehicle type (scooter, van, or truck). The platform features real-time GPS tracking, intelligent driver matching, integrated payments via Paynow (card, EcoCash, OneMoney), and push notifications — all across two Android apps and a hybrid Go + Django backend.

---

## Project Structure

```
TumaGo/
├── backend/                 # All backend services
│   ├── django/              # Django REST API (auth, CRUD, admin)
│   ├── go_services/         # High-performance Go microservices
│   │   ├── gateway/         # API gateway & reverse proxy
│   │   ├── location/        # Real-time GPS WebSocket service
│   │   └── matching/        # Driver matching engine
│   ├── notification/        # FCM push notification service (FastAPI)
│   ├── infrastructure/      # Kubernetes, nginx, monitoring configs
│   ├── docker-compose.yml   # Development environment
│   └── docker-compose.prod.yml  # Production with replicas
├── TumaGo_client/           # Android app for senders
└── TumaGo_driver/           # Android app for drivers
```

---

## Backend Architecture

```
Mobile Apps → Go Gateway (:80) → Django (:8000)      — auth, users, deliveries, admin
                                → Go Location (:8001) — real-time driver GPS
                                → Go Matching (:8002) — nearest driver search
                                → Notification (:8003) — push notifications
```

### Go Gateway Service
The API entry point that replaces nginx for routing. Handles all incoming traffic and distributes it to the correct backend service.
- **Per-IP rate limiting** — 60 req/min general, 10 req/min for auth and delivery endpoints
- **JWT validation** for WebSocket connections
- **WebSocket proxy** — upgrades and forwards driver GPS connections to the location service
- **Internal endpoint protection** — blocks external access to `/internal/match/`

### Go Location Service
Handles real-time GPS streaming from drivers over WebSocket connections.
- Drivers connect via `ws://host/ws/driver_location?token=<JWT>`
- Each GPS update stores the driver's position in **Redis GEO** for instant spatial queries
- Simultaneously upserts the driver's location in PostgreSQL (one row per driver)
- Designed to handle **100k+ concurrent WebSocket connections** using Go goroutines (~4KB per connection vs ~1MB in Python)
- Removes drivers from the live index on disconnect

### Go Matching Service
Finds the closest available driver when a delivery is requested.
- **Redis GEOSEARCH** finds all drivers within a 15km radius, sorted by distance
- **Single PostgreSQL query** filters by vehicle type and availability
- Returns the nearest valid driver with coordinates and distance
- Called internally by Django's Dramatiq background workers

### Django Backend
Handles all business logic, authentication, data management, and payments.
- Custom **JWT authentication** (HS256) — shared secret key with Go services
- User and driver registration, login, profile management
- Delivery creation, acceptance, cancellation, and completion
- **Paynow payment integration** — card, EcoCash, and OneMoney via Zimbabwe's leading payment gateway
- Driver balance tracking — automatically calculates platform charges (10–50% by vehicle type) and tracks amounts owed to drivers
- Admin payout endpoint for settling driver balances
- **Dramatiq background workers** for async driver matching with Redis pub/sub
- Django Admin panel for operations
- Swagger/ReDoc API documentation

### Notification Service (FastAPI)
Sends Firebase Cloud Messaging (FCM) push notifications.
- Subscribes to Redis pub/sub channels for real-time event processing
- Also exposes a direct HTTP endpoint for Django to call
- Stays in Python because FCM calls are I/O-bound — Go wouldn't improve performance

---

## Tech Stack

| Component | Technology |
|-----------|-----------|
| API Gateway | Go 1.22, stdlib `net/http`, `x/time/rate` |
| Location Service | Go 1.22, gorilla/websocket, go-redis, pgx |
| Matching Service | Go 1.22, go-redis, pgx |
| Backend API | Django 5.2, Django REST Framework 3.16 |
| Notifications | FastAPI, Firebase Admin SDK |
| Payments | Paynow SDK (card, EcoCash, OneMoney) |
| Background Tasks | Dramatiq + Redis |
| Database | PostgreSQL 15 + PgBouncer |
| Cache / Queue / GEO | Redis 7 |
| Android Apps | Java, Retrofit 2.9, OkHttp, Google Maps SDK |
| Containerization | Docker, Docker Compose, Kubernetes |
| Monitoring | Prometheus + Grafana |

---

## Getting Started

### Prerequisites
- **Docker** and **Docker Compose** (that's it — Go, Python, and all dependencies are built inside containers)

### Run the backend

```bash
cd backend
docker compose up -d --build
```

This starts all services: Go gateway, location, matching, Django, Dramatiq workers, Redis, PostgreSQL, PgBouncer, Prometheus, and Grafana.

### Run the Android apps

```bash
# Client app
cd TumaGo_client
./gradlew assembleDebug

# Driver app
cd TumaGo_driver
./gradlew assembleDebug
```

### Environment Variables

Create a `.env` file at `backend/django/TumaGo/.env` with:

```env
SECRET_KEY=your-secret-key
DATABASE_URL=postgresql://postgres:postgres@pgbouncer:5432/postgres
REDIS_URL=redis://redis:6379
GOOGLE_MAPS_API_KEY=your-google-maps-key
FIREBASE_CREDENTIALS={"type":"service_account",...}
PAYNOW_INTEGRATION_ID=your-paynow-id
PAYNOW_INTEGRATION_KEY=your-paynow-key
PAYNOW_RETURN_URL=https://yourdomain.com/payment-return
PAYNOW_RESULT_URL=https://yourdomain.com/api/v1/payment/callback/
```

---

## Key Features

### Client App
- Book deliveries by selecting vehicle type (scooter, van, truck)
- Real-time price calculation based on distance
- **Pay online** via card (WebView checkout), EcoCash, or OneMoney — or choose cash
- Payment status polling with automatic retry (up to 2 minutes)
- Live track your driver on Google Maps
- View delivery history with status
- Rate drivers after delivery

### Driver App
- Register with license upload and vehicle registration
- Accept or decline delivery requests via push notification
- Navigate to pickup and dropoff using Google Maps
- Track earnings with daily, weekly, and monthly finance breakdowns
- **View balance owed** by the platform for non-cash deliveries
- Automatic availability management

### Shared
- JWT-based authentication with encrypted token storage
- Real-time GPS tracking over WebSocket
- Firebase push notifications
- Material Design 3 UI with TumaGo branding

---

## Payment Flow

```
User selects payment method
  ├── Cash → skip payment, go straight to driver matching
  └── Card / EcoCash / OneMoney
        ├── POST /payment/initiate/ → Paynow SDK
        ├── Card: WebView checkout page
        │   EcoCash/OneMoney: USSD prompt sent to phone
        ├── Client polls /payment/status/ every 3s (max 40 attempts)
        └── Once paid → delivery request → driver matching
```

**Platform charges** are deducted from driver earnings on delivery completion:

| Vehicle | Platform Charge |
|---------|----------------|
| Scooter | 20% |
| Van | 30% |
| Truck | 50% |

For non-cash payments, the driver's net earnings are tracked in a `DriverBalance` record. Admins settle balances via the `/payment/pay-driver/` endpoint.

---

## Production Deployment

```bash
cd backend
docker compose -f docker-compose.prod.yml up -d --build
```

Production runs with replicas: Gateway (2x), Location (3x), Matching (2x), Django (3x), Workers (2x). Kubernetes manifests are available in `backend/infrastructure/kubernetes/`.

---

## Author

**Brandon Khumalo**
Backend & Mobile Developer
[LinkedIn](https://www.linkedin.com/in/brandon-khumalo04) | [Email](mailto:brandonkhumz40@gmail.com)

---

## License

This project is licensed under the [MIT License](LICENSE).
