# TumaGo — Deploy to AWS EC2 (t3.small)

Single-instance deployment for testing and demos. Costs ~$0.53/day (~$16/month).
Everything is done through the AWS web console — no local tools required.

---

## Prerequisites

- AWS account with $100 credits or free tier
- Firebase project with FCM enabled
- Google Maps API key
- GitHub repo access (HTTPS)

---

## 1. Launch EC2 Instance

1. Go to **[AWS Console → EC2 → Launch Instance](https://console.aws.amazon.com/ec2/home#LaunchInstances)**
2. Configure:

| Setting | Value |
|---------|-------|
| Name | `tumago-backend` |
| AMI | Ubuntu 22.04 LTS (Free tier eligible) |
| Instance type | `t3.small` (2 vCPU, 2 GB RAM) |
| Key pair | **Proceed without a key pair** (we'll use browser SSH) |
| Storage | 20 GB gp3 |

3. **Network Settings** → click **Edit**, then **Add security group rule** for each:

| Type | Port | Source | Purpose |
|------|------|--------|---------|
| SSH | 22 | 0.0.0.0/0 | EC2 Instance Connect |
| HTTP | 80 | 0.0.0.0/0 | API gateway |

4. Click **Launch Instance**
5. Wait until **Instance State** shows **Running** (1-2 minutes)

---

## 2. Connect via Browser

1. Go to **EC2 → Instances** → select `tumago-backend`
2. Click **Connect** (top right)
3. Select **EC2 Instance Connect** tab
4. Leave username as `ubuntu`
5. Click **Connect**

A terminal opens in your browser. All commands below are run in this terminal.

---

## 3. Install Docker

```bash
# Install Docker
curl -fsSL https://get.docker.com | sh
sudo usermod -aG docker $USER

# Apply group change without logging out
newgrp docker

# Verify Docker works
docker --version
docker compose version
```

---

## 4. Clone and Configure

```bash
# Clone the repo
git clone https://github.com/Brandonkhumalo/TumaGo
cd TumaGo/backend

# Create .env from template
cp .env.example .env
nano .env
```

Fill in the `.env` values:

```env
# Generate a random secret key
SECRET_KEY=<run: python3 -c "import secrets; print(secrets.token_urlsafe(64))">

# Database password
POSTGRES_PASSWORD=<strong-random-password>

# Set to your EC2 public IP
ALLOWED_HOSTS=<EC2_PUBLIC_IP>

# Your Google Maps API key
GOOGLE_MAPS_API_KEY=<your-key>

# Firebase — paste entire JSON service account key as one line
FIREBASE_CREDENTIALS={"type":"service_account","project_id":"..."}
```

### Getting Firebase credentials

1. Go to [Firebase Console](https://console.firebase.google.com/)
2. Select your project → **Project Settings** → **Service Accounts**
3. Click **Generate New Private Key**
4. Open the downloaded JSON file
5. Copy the entire contents and paste as the `FIREBASE_CREDENTIALS` value (single line)

---

## 5. Deploy

```bash
# Build all Docker images
docker compose -f docker-compose.t3small.yml build

# Start database and Redis first
docker compose -f docker-compose.t3small.yml up -d db redis pgbouncer

# Wait for database to be healthy (~10 seconds)
sleep 10

# Run Django migrations
docker compose -f docker-compose.t3small.yml run --rm web python manage.py migrate

# Create a Django admin user (optional)
docker compose -f docker-compose.t3small.yml run --rm web python manage.py createsuperuser

# Start all services
docker compose -f docker-compose.t3small.yml up -d
```

Or use the deploy script:

```bash
chmod +x deploy-ec2.sh
./deploy-ec2.sh
```

---

## 6. Verify

```bash
# Check all services are running
docker compose -f docker-compose.t3small.yml ps

# Health check
curl http://localhost/health

# Check logs if something is wrong
docker compose -f docker-compose.t3small.yml logs -f

# Check a specific service
docker compose -f docker-compose.t3small.yml logs web
docker compose -f docker-compose.t3small.yml logs gateway
```

Expected output from `ps`:

```
NAME        STATUS
gateway     Up
location    Up
matching    Up
web         Up
worker      Up
notification Up
redis       Up
db          Up (healthy)
pgbouncer   Up
```

---

## 7. Find Your Public IP

1. Go to **EC2 → Instances** → select `tumago-backend`
2. Copy the **Public IPv4 address** from the details panel

Or from the browser terminal:

```bash
curl http://169.254.169.254/latest/meta-data/public-ipv4
```

---

## 8. Configure Android Apps

On your local machine, update `ApiClient.java` in both apps:

**TumaGo_client** and **TumaGo_driver**:

```java
// Change from localhost to your EC2 IP
private static final String BASE_URL = "http://<EC2_PUBLIC_IP>/";
```

Rebuild and install:

```bash
cd TumaGo_client && ./gradlew assembleDebug
cd TumaGo_driver && ./gradlew assembleDebug
```

---

## Common Commands

```bash
cd ~/TumaGo/backend

# View all logs
docker compose -f docker-compose.t3small.yml logs -f

# Restart a specific service
docker compose -f docker-compose.t3small.yml restart web

# Stop everything
docker compose -f docker-compose.t3small.yml down

# Stop and delete all data (database, redis)
docker compose -f docker-compose.t3small.yml down -v

# Rebuild after code changes
git pull
docker compose -f docker-compose.t3small.yml up -d --build

# Run Django management commands
docker compose -f docker-compose.t3small.yml run --rm web python manage.py migrate
docker compose -f docker-compose.t3small.yml run --rm web python manage.py createsuperuser
docker compose -f docker-compose.t3small.yml run --rm web python manage.py shell

# Check memory usage
docker stats --no-stream

# Django admin panel
# http://<EC2_PUBLIC_IP>/admin/
```

---

## Troubleshooting

### Services keep restarting

```bash
# Check which service is failing
docker compose -f docker-compose.t3small.yml ps

# Read its logs
docker compose -f docker-compose.t3small.yml logs <service-name>
```

### Database connection errors

PgBouncer may start before PostgreSQL is ready. Restart it:

```bash
docker compose -f docker-compose.t3small.yml restart pgbouncer
```

### Out of memory

Check what's using RAM:

```bash
docker stats --no-stream
free -h
```

If tight on memory, stop optional services:

```bash
# Notification service can be stopped if not testing FCM
docker compose -f docker-compose.t3small.yml stop notification
```

### Can't connect from Android app

1. Check EC2 security group allows port 80 from `0.0.0.0/0`
2. Verify `ALLOWED_HOSTS` in `.env` includes `*` or your EC2 IP
3. Make sure `BASE_URL` in `ApiClient.java` uses `http://` not `https://`

### Browser terminal disconnected

Just reconnect: **EC2 → Instances** → select instance → **Connect** → **EC2 Instance Connect** → **Connect**. Your services are still running — Docker keeps them alive.

### Updating after code changes

```bash
cd ~/TumaGo
git pull
cd backend
docker compose -f docker-compose.t3small.yml up -d --build
```

---

## Cost Summary

| Resource | Monthly Cost |
|----------|-------------|
| EC2 t3.small (on-demand) | ~$15.00 |
| EBS 20 GB gp3 | ~$1.60 |
| Data transfer (minimal) | ~$0.50 |
| **Total** | **~$17/month** |

Your $100 AWS credits will last approximately **5-6 months**.

---

## Switching to Production

When ready for production, use the full compose file with replicas:

```bash
docker compose -f docker-compose.prod.yml up -d --build
```

This requires a larger instance (t3.medium or bigger) and optionally managed RDS + ElastiCache.
