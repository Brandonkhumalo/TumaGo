# TumaGo — Production Readiness Report

**Date**: 2026-03-14
**Status**: NOT READY — Critical issues must be resolved before launch

---

## Table of Contents

1. [Critical Issues (Must Fix)](#1-critical-issues-must-fix)
2. [Backend](#2-backend)
3. [Client App (Sender)](#3-client-app-sender)
4. [Driver App](#4-driver-app)
5. [Infrastructure & DevOps](#5-infrastructure--devops)
6. [Missing Features](#6-missing-features)
7. [Summary & Priority Order](#7-summary--priority-order)

---

## 1. Critical Issues (Must Fix)

These are **production blockers** — ship with any of these and you risk data loss, security breaches, or app rejection.

### 1.1 Dangerous Admin Endpoints (Backend)
- **Files**: `backend/django/TumaGo_Server/urls.py:28-30`, `views/UserViews/views.py:128-152`
- `/allUsers/` — returns all user PII with `@permission_classes([AllowAny])`
- `/delete/` — **deletes every user in the database** with `@permission_classes([AllowAny])`
- `/allDeliveries/` — exposes all delivery data publicly
- **Fix**: DELETE these endpoints or restrict to `IsAuthenticated + IsAdminUser`

### 1.3 Cleartext Traffic Enabled (Both Apps)
- **Files**: `TumaGo_client/AndroidManifest.xml:19`, `TumaGo_driver/AndroidManifest.xml:19`
- `android:usesCleartextTraffic="true"` — all traffic can be sent unencrypted
- **Fix**: Set to `false`, create `network_security_config.xml` allowing cleartext only for `localhost`

### 1.4 WebSocket Uses ws:// Not wss:// (Driver App)
- **File**: `TumaGo_driver/activities/MainActivity.java:147`
- `ws://13.246.35.254/ws/driver_location/?token=...` — unencrypted, hardcoded IP, token in URL
- **Fix**: Use `wss://` with domain name, pass token in WebSocket header

### 1.5 No Code Obfuscation (Both Apps)
- **Files**: `TumaGo_client/app/build.gradle.kts:45`, `TumaGo_driver/app/build.gradle.kts:45`
- `isMinifyEnabled = false` — APKs trivially decompiled, all API endpoints/keys exposed
- **Fix**: Enable R8 minification, write ProGuard rules for Retrofit/Gson/OkHttp

### 1.6 No Crash Reporting (Both Apps)
- Neither app has Firebase Crashlytics or any error tracking
- Exceptions use `e.printStackTrace()` (logcat only)
- **Fix**: Add `firebase-crashlytics` dependency, initialize in Application class

### 1.7 No App Signing Configuration (Both Apps)
- No `signingConfigs` block in either `build.gradle.kts`
- Can't produce signed release APKs for Play Store
- **Fix**: Add release signing config using env vars for keystore credentials

### 1.9 No Database Backup Strategy
- PostgreSQL data in Docker volumes only — no automated backups
- Redis persistence disabled (`--save ""`) in all compose files
- **Fix**: Use AWS RDS automated backups or add pg_dump cron, enable Redis AOF in prod

### 1.10 No CI/CD Pipeline
- No GitHub Actions, no Jenkinsfile, no automated testing or deployment
- Only manual `deploy-ec2.sh` script
- **Fix**: Set up GitHub Actions for test → build → deploy

---

## 2. Backend

### 2.1 Security

| Issue | File | Severity |
|-------|------|----------|
| CORS wildcard `allow_origins=["*"]` on notification service | `notification/main.py:116` | HIGH |
| `DEBUG` defaults to `True` in dev compose | `docker-compose.yml:79` | MEDIUM |
| `ALLOWED_HOSTS` defaults to `*` | `docker-compose.yml:80` | MEDIUM |
| Token passed in WebSocket query parameter | `go_services/location/ws.go:31` | MEDIUM |
| Firebase credentials as env var JSON blob | `docker-compose.yml:82` | MEDIUM |
| Django admin exposed on same port as API | `django/TumaGo/urls.py:19` | MEDIUM |
| `print()` statements log sensitive data (passwords, tokens) | Multiple Django views | MEDIUM |
| Malformed print statements with broken quotes | `views/UserViews/views.py:78,84,88,92` | LOW |


### 2.3 Database

| Issue | File | Severity |
|-------|------|----------|
| No unique constraint on `DriverLocations.driver` | `go_services/location/db.go:57-85` | MEDIUM |
| N+1 query risk in delivery acceptance | `views/DriverViews/driverViews.py:193` | MEDIUM |
| PgBouncer pool size may be too low for prod replicas | `docker-compose.prod.yml:211` | LOW |

---

## 3. Client App (Sender)

### 3.1 Security

| Issue | File | Severity |
|-------|------|----------|
| Hardcoded Maps API key (see 1.2) | `AndroidManifest.xml:58-60` | CRITICAL |
| Cleartext traffic (see 1.3) | `AndroidManifest.xml:19` | CRITICAL |
| No minification (see 1.5) | `build.gradle.kts:45` | CRITICAL |
| No network security config (no cert pinning) | Missing file | HIGH |
| `e.printStackTrace()` in production (Token.java, adapters) | Multiple files | MEDIUM |
| `System.out.println()` logs driver data | `DeliveryDetails.java:129` | LOW |
| Firebase project is "roadlinksolutions" not "tumago" | `google-services.json` | MEDIUM |


### 3.5 Build Configuration

| Issue | File | Severity |
|-------|------|----------|
| No signing config (see 1.7) | `build.gradle.kts` | CRITICAL |
| No build flavors (debug/staging/prod) | `build.gradle.kts` | HIGH |
| `versionCode = 1` hardcoded, no auto-increment | `build.gradle.kts:34-35` | HIGH |
| Empty ProGuard rules | `proguard-rules.pro` | HIGH |
| No lint configuration | `build.gradle.kts` | MEDIUM |
| Data backup rules not configured (tokens could leak) | `data_extraction_rules.xml` | MEDIUM |

---

## 4. Driver App

### 4.1 Security

| Issue | File | Severity |
|-------|------|----------|
| Hardcoded Maps API key (see 1.2) | `AndroidManifest.xml:90` | CRITICAL |
| Cleartext traffic (see 1.3) | `AndroidManifest.xml:19` | CRITICAL |
| ws:// with hardcoded IP and token in URL (see 1.4) | `MainActivity.java:147` | CRITICAL |
| No minification (see 1.5) | `build.gradle.kts:45` | CRITICAL |
| `ApiClient2.java` always logs at BODY level (no debug check) | `ApiClient2.java:14` | MEDIUM |
| Logs GPS coordinates without debug check | `MainActivity.java:183` | MEDIUM |

### 4.6 Build Configuration

Same issues as Client App (no signing, no flavors, no version increment, empty ProGuard).

---

## 5. Infrastructure & DevOps

### 5.1 Docker Security

| Issue | File | Severity |
|-------|------|----------|
| All Python containers run as root | Django + Notification Dockerfiles | HIGH |
| No `HEALTHCHECK` directive in Dockerfiles | All Dockerfiles | MEDIUM |
| No security context in K8s manifests (`runAsNonRoot`, etc.) | All K8s deployments | HIGH |

### 5.2 Docker Compose (Production)

| Issue | File | Severity |
|-------|------|----------|
| Redis persistence disabled (`--save ""`) | `docker-compose.prod.yml:171` | HIGH |
| Gateway limited to 1 replica (single point of failure) | `docker-compose.prod.yml:50` | MEDIUM |
| Gateway memory limit 128M may be insufficient under load | `docker-compose.prod.yml:53-54` | MEDIUM |
| No TLS termination in compose (relies on external nginx/ingress) | `docker-compose.prod.yml` | MEDIUM |

### 5.3 Kubernetes

| Issue | File | Severity |
|-------|------|----------|
| No `securityContext` on any deployment | All K8s manifests | HIGH |
| No `NetworkPolicy` resources (all pods can talk to all pods) | Missing | MEDIUM |
| No `PodDisruptionBudget` for graceful scaling | Missing | MEDIUM |
| Redis is single replica (comment says "replace with cluster") | `redis-cluster.yaml:9` | MEDIUM |
| CPU requests very low for gateway (50m) and location (100m) | K8s gateway/location yamls | MEDIUM |
| Notification service missing liveness probe | `notification-service.yaml` | LOW |
| PgBouncer missing readiness/liveness probes | `pgbouncer.yaml` | LOW |

### 5.4 Monitoring & Observability

| Issue | File | Severity |
|-------|------|----------|
| Prometheus config references 3 exporters that aren't deployed | `monitoring/prometheus.yml` | HIGH |
| No application-level metrics exported by any service | All services | MEDIUM |
| No Grafana dashboards provisioned | `monitoring/grafana-datasources.yaml` | MEDIUM |
| No alerting rules configured | Missing | MEDIUM |
| No centralized logging (ELK, Loki, etc.) | Missing | MEDIUM |
| No distributed tracing (Jaeger, etc.) | Missing | LOW |
| Django logs to console only | `settings.py:184-199` | LOW |

### 5.5 Deployment

| Issue | File | Severity |
|-------|------|----------|
| No CI/CD pipeline (see 1.10) | Missing | CRITICAL |
| Deploy script is fully manual, no rollback | `deploy-ec2.sh` | HIGH |
| Database migrations are manual | `deploy-ec2.sh:61` | MEDIUM |
| Secrets managed manually (kubectl create, .env files) | K8s secrets-template | MEDIUM |

---

## 6. Missing Features

### 6.1 Business-Critical (Production Blockers)

| Feature | Status | Notes |
|---------|--------|-------|
| **Payment processing** | NOT IMPLEMENTED | UI shows "Cash"/"Card" options but no payment gateway (Stripe, etc.) |
| **Email verification** | INCOMPLETE | `verifiedEmail` field exists but no verification endpoint |
| **In-app updates** | MISSING | No Google Play In-App Update API — users stuck on buggy versions |

### 6.2 Important (Should Have at Launch)

| Feature | Status | Notes |
|---------|--------|-------|
| Crash reporting | MISSING | No Crashlytics — blind to production crashes |
| Firebase Analytics | MISSING | No user behavior tracking |
| Deep linking | MISSING | Push notifications can't open specific screens |
| Offline caching | MISSING | Apps are online-only, no graceful degradation |
| Driver background tracking | MISSING | Location stops when driver app backgrounded |
| In-app support/help | MISSING | No way for users to report issues |
| App version management | MISSING | `versionCode = 1` hardcoded |

### 6.3 Nice to Have (Post-Launch)

| Feature | Status | Notes |
|---------|--------|-------|
| Firebase Performance Monitoring | MISSING | No screen load time or API latency tracking |
| Rate app prompt | MISSING | No Google Play In-App Review |
| Driver onboarding tutorial | MISSING | No first-run experience |
| API versioning | MISSING | Breaking changes force all clients to update |
| Certificate pinning | MISSING | OkHttp supports it but not configured |

---

## 7. Summary & Priority Order

### Work Breakdown

| Category | Critical | High | Medium | Low | Total |
|----------|----------|------|--------|-----|-------|
| Security | 7 | 6 | 10 | 2 | 25 |
| Stability | 0 | 6 | 4 | 1 | 11 |
| UI/UX | 0 | 3 | 8 | 1 | 12 |
| Infrastructure | 2 | 5 | 10 | 3 | 20 |
| Missing Features | 3 | 7 | 0 | 5 | 15 |
| Build Config | 2 | 4 | 3 | 0 | 9 |
| **Total** | **14** | **31** | **35** | **12** | **92** |

### Recommended Fix Order

**Phase 1 — Security & Data Safety (Do First)**
1. Delete/restrict dangerous admin endpoints (`/delete/`, `/allUsers/`, `/allDeliveries/`)
2. Remove hardcoded API keys from manifests → use BuildConfig
3. Disable cleartext traffic, create `network_security_config.xml`
4. Switch WebSocket to `wss://` with domain, move token to header
5. Enable R8 minification + write ProGuard rules
6. Add database indexes on queried columns
7. Set up PostgreSQL backups
8. Replace `print()` with proper logging
9. Fix CORS wildcard on notification service
10. Use file path for Firebase credentials (not env var JSON)

**Phase 2 — Stability & Build (Do Second)**
1. Fix all NPE/crash risks in both Android apps
2. Add crash reporting (Firebase Crashlytics)
3. ~~Add `POST_NOTIFICATIONS` permission to both apps~~ ✅ DONE
4. Configure app signing for release builds
5. Add build flavors (debug/staging/production)
6. Set up CI/CD (GitHub Actions)
7. Add loading states and error handling to UI
8. Add non-root user to Docker containers
9. Auto-increment `versionCode`
10. Write ProGuard keep rules for Retrofit/Gson models

**Phase 3 — Features & Polish (Do Third)**
1. Implement payment processing or remove "Card" option
2. Complete email verification flow
3. Add Google Play In-App Updates
4. Implement driver background location service
5. Add Firebase Analytics
6. Add deep linking for push notifications
7. Add offline caching for delivery lists
8. Implement monitoring (deploy Prometheus exporters, Grafana dashboards)
9. Add K8s security contexts and network policies
10. Enable Redis persistence in production

**Phase 4 — Production Hardening (Before Going Live)**
1. Security audit with obfuscated APK
2. Load testing (verify rate limits, connection pools, HPA scaling)
3. Disaster recovery drill (restore from backup)
4. Accessibility pass (content descriptions on all interactive elements)
5. Test on Android 12/13/14/15 for permission compatibility
6. Set up alerting rules in Prometheus/Grafana
7. Document deployment and rollback procedures
8. Configure secrets management (AWS Secrets Manager or K8s Sealed Secrets)
