# Differences Between `main` and `Payments` Branches

**1 commit, 37 files changed, +2,843 lines, -41 lines**

---

## Summary

The `main` branch has the old flat-fee driver charge system ($0.10-$0.50 per delivery based on vehicle type). The `Payments` branch replaces it with an **inDrive-style prepaid wallet commission system**, admin-configurable pricing, and refund reviews.

---

## What Changed

### 1. Driver Wallet Commission System (replaces flat vehicle charges)

| | `main` | `Payments` |
|---|---|---|
| **How drivers pay** | Flat fee deducted after delivery ($0.10-$0.50 based on vehicle) | % commission deducted from prepaid wallet at trip acceptance |
| **Cash deliveries** | $0.10-$0.50 flat charge | 15% of fare from wallet |
| **Online deliveries** | $0.10-$0.50 flat charge | 20% of fare from wallet |
| **When charged** | After trip ends | At trip acceptance (before delivery starts) |
| **Owed balance (online)** | fare - flat charge added to owed balance | 100% of fare added to owed balance |
| **Driver gating** | None — any driver can accept any trip | Must have enough wallet balance to cover commission |

### 2. New Database Models (3 new + 1 new)

| Model | Purpose |
|-------|---------|
| `DriverWallet` | Prepaid balance per driver (OneToOne) |
| `WalletTransaction` | Audit trail: topup, commission, refund, transfer_in, transfer_out |
| `CommissionRefundRequest` | Driver cancellation refund requests (pending/approved/denied by admin) |
| `PlatformSettings` | Singleton for admin-configurable pricing and commission rates |

### 3. New Django API Endpoints (11 new)

**Driver Wallet (6):**
- `POST /driver/wallet/topup/` — initiate Paynow top-up ($5-$100)
- `GET /driver/wallet/topup/status/` — poll payment, credit wallet on success
- `GET /driver/wallet/balance/` — both wallet + owed balances
- `POST /driver/wallet/transfer/` — move money between wallet and owed balance
- `GET /driver/wallet/transactions/` — paginated transaction history
- `GET /driver/wallet/refund-requests/` — driver's own refund requests

**Driver Cancel (1):**
- `POST /driver/cancel/delivery/` — cancel with required reason, creates refund request

**Admin Refund Reviews (3):**
- `GET /admin/refund-requests/` — list with status filter
- `POST /admin/refund-requests/<id>/review/` — approve or deny
- `GET /admin/refund-requests/metrics/` — pending/approved/denied counts

**Admin Platform Settings (2):**
- `GET /admin/settings/` — current pricing and commission config
- `PUT /admin/settings/update/` — update any settings field

### 4. Modified Django Views

| File | What Changed |
|------|-------------|
| `AcceptTrip` (driverViews.py) | Deducts commission from wallet before creating delivery. Rejects if insufficient balance. |
| `end_trip` (driverViews.py) | Removed flat vehicle charges. Online payments now add 100% fare to owed balance. Commission rates read from PlatformSettings. |
| `cancel_delivery` (userViews.py) | Client cancel now auto-refunds commission to driver wallet. |
| `GetTripExpenses` (userViews.py) | Per-km pricing now reads from PlatformSettings instead of hardcoded values. |
| `driver_register` (authViews.py) | Auto-creates DriverWallet (balance=0) on driver signup. |
| `TripData` / `find_nearest_driver` (delivery.py) | Filters drivers by wallet balance >= commission amount. |

### 5. New Driver Cancel Flow

**`main`:** Only clients can cancel deliveries. No refund concept.

**`Payments`:**
- **Client cancels** → commission auto-refunded to driver wallet instantly
- **Driver cancels** → must provide a reason → `CommissionRefundRequest` created (pending) → admin reviews → approve (money returned) or deny (commission kept)

### 6. Go Gateway — New Routes

```
/api/v1/driver/wallet/*         → Django (general rate limit)
/api/v1/driver/cancel/delivery/ → Django (general rate limit)
/api/v1/admin/refund-requests/* → Django (general rate limit)
/api/v1/admin/settings/*        → Django (general rate limit)
```

### 7. Go Matching Service — Wallet Balance Filter

| | `main` | `Payments` |
|---|---|---|
| `matchRequest` struct | origin, vehicle, trip_id | + fare, payment_method |
| `findAvailableDrivers` | Filters by availability + vehicle type | + LEFT JOIN DriverWallet, filter by `balance >= commission` |
| Commission calc | N/A | 15% cash / 20% online, calculated in handler |

### 8. Driver Android App — New Screens

**New Activities:**
- `WalletActivity` — two balance cards (wallet + owed), top-up via Paynow, transfer between accounts, transaction history
- `RefundRequestsActivity` — list of refund requests with status badges

**New Layouts:**
- `activity_wallet.xml`, `activity_refund_requests.xml`
- `dialog_topup.xml` (EcoCash/OneMoney/Card selection + phone input)
- `dialog_transfer.xml` (direction picker + amount)
- `item_wallet_transaction.xml`, `item_refund_request.xml`

**New Models (7):**
- `WalletBalance`, `WalletTransaction`, `WalletTransactionResponse`
- `WalletTopupResponse`, `WalletTopupStatus`
- `RefundRequest`, `RefundRequestResponse`

**New API Endpoints in ApiService (7):**
- `getWalletBalance`, `walletTopup`, `walletTopupStatus`
- `walletTransfer`, `getWalletTransactions`
- `getRefundRequests`, `driverCancelDelivery`

**Navigation:** "Wallet" menu item added to main drawer (between Deliveries and Finances)

### 9. Admin Next.js Dashboard — New Pages

**Refund Requests** (`/tumago/admin/refunds`):
- Metrics cards (pending/approved/denied/total refunded)
- Filterable table by status
- Inline approve/deny with admin notes

**Platform Settings** (`/tumago/admin/settings`):
- Vehicle pricing: per-km rate + base fee for scooter/van/truck
- Commission rates: cash % and online %
- Save button with validation

**Sidebar:** 2 new nav items added (Settings, Refund Requests)

### 10. Pricing Now Admin-Configurable

| Setting | Default | Where Used |
|---------|---------|------------|
| `scooter_price_per_km` | $0.50 | GetTripExpenses |
| `scooter_base_fee` | $0.20 | GetTripExpenses |
| `van_price_per_km` | $1.10 | GetTripExpenses |
| `van_base_fee` | $0.30 | GetTripExpenses |
| `truck_price_per_km` | $2.30 | GetTripExpenses |
| `truck_base_fee` | $0.50 | GetTripExpenses |
| `cash_commission_pct` | 15% | AcceptTrip, end_trip, matching |
| `online_commission_pct` | 20% | AcceptTrip, end_trip, matching |

---

## Files Changed

### New Files (22)
```
backend/django/TumaGo_Server/migrations/0013_wallet_commission_system.py
backend/django/TumaGo_Server/migrations/0014_platform_settings.py
backend/django/TumaGo_Server/views/PaymentViews/walletViews.py
frontend/src/app/tumago/admin/(dashboard)/refunds/page.tsx
frontend/src/app/tumago/admin/(dashboard)/settings/page.tsx
TumaGo_driver/.../activities/WalletActivity.java
TumaGo_driver/.../activities/RefundRequestsActivity.java
TumaGo_driver/.../helpers/WalletTransactionAdapter.java
TumaGo_driver/.../helpers/RefundRequestAdapter.java
TumaGo_driver/.../models/WalletBalance.java
TumaGo_driver/.../models/WalletTransaction.java
TumaGo_driver/.../models/WalletTransactionResponse.java
TumaGo_driver/.../models/WalletTopupResponse.java
TumaGo_driver/.../models/WalletTopupStatus.java
TumaGo_driver/.../models/RefundRequest.java
TumaGo_driver/.../models/RefundRequestResponse.java
TumaGo_driver/.../res/layout/activity_wallet.xml
TumaGo_driver/.../res/layout/activity_refund_requests.xml
TumaGo_driver/.../res/layout/dialog_topup.xml
TumaGo_driver/.../res/layout/dialog_transfer.xml
TumaGo_driver/.../res/layout/item_wallet_transaction.xml
TumaGo_driver/.../res/layout/item_refund_request.xml
```

### Modified Files (15)
```
backend/django/TumaGo_Server/models.py
backend/django/TumaGo_Server/urls.py
backend/django/TumaGo_Server/views/AdminViews/admin_views.py
backend/django/TumaGo_Server/views/DriverViews/authViews.py
backend/django/TumaGo_Server/views/DriverViews/deliveryMatching/delivery.py
backend/django/TumaGo_Server/views/DriverViews/driverViews.py
backend/django/TumaGo_Server/views/UserViews/userViews.py
backend/go_services/gateway/main.go
backend/go_services/matching/db.go
backend/go_services/matching/handler.go
frontend/src/app/tumago/admin/(dashboard)/layout.tsx
frontend/src/lib/api.ts
TumaGo_driver/.../Interface/ApiService.java
TumaGo_driver/.../activities/MainActivity.java
TumaGo_driver/.../AndroidManifest.xml
```
