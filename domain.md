# TumaGo Domain Setup — tumago.co.zw

## Domain Info

- **Domain:** tumago.co.zw
- **Registrar:** Zimbabwean registrar
- **Elastic IP:** already provisioned in AWS

---

## What's Automated

Everything below happens automatically on every `git push` to `main`:

- Builds all backend services + frontend via Docker
- Copies the correct nginx config to EC2 (picks HTTPS version if SSL certs exist, HTTP otherwise)
- Reloads nginx

You don't need to touch Docker, nginx configs, or service restarts — just push.

---

## What You Need To Do (One-Time)

There are only **5 manual steps**. Do them in order.

### 1. DNS — Point domain to your Elastic IP

Log into your **domain registrar dashboard** and create two A records:

| Type | Name        | Value              | TTL |
|------|-------------|--------------------|-----|
| A    | `@` (blank) | `<your-elastic-ip>` | 300 |
| A    | `www`       | `<your-elastic-ip>` | 300 |

Verify it worked (may take up to 1 hour):

```bash
nslookup tumago.co.zw
```

### 2. AWS — Confirm Elastic IP + Security Group

**Elastic IP:** AWS Console → EC2 → Elastic IPs → confirm it's associated with your instance. If not: Actions → Associate.

**Security Group:** EC2 → Security Groups → add inbound rules:

| Port | Source      | Purpose  |
|------|-------------|----------|
| 80   | 0.0.0.0/0  | HTTP     |
| 443  | 0.0.0.0/0  | HTTPS    |
| 22   | Your IP     | SSH      |

### 3. EC2 — Install nginx (once)

```bash
ssh -i your-key.pem ubuntu@<your-elastic-ip>
sudo apt update && sudo apt install nginx -y
sudo mkdir -p /var/www/certbot
```

That's it. The deploy workflow handles all nginx configuration from the repo automatically.

### 4. Push to main, then install SSL

Push your code. The deploy will start the frontend + backend and set up the HTTP nginx config.

Once DNS has propagated and the site is reachable on HTTP, install the free SSL cert:

```bash
sudo apt install certbot python3-certbot-nginx -y
sudo certbot --nginx -d tumago.co.zw -d www.tumago.co.zw
```

Follow the prompts. After this, every future deploy auto-uses the HTTPS config.

Verify auto-renewal works:

```bash
sudo certbot renew --dry-run
```

### 5. Update .env on EC2

The `.env` file is gitignored, so you must edit it manually on the server:

```bash
nano ~/TumaGo/backend/.env
```

Change these three lines:

```
ALLOWED_HOSTS=tumago.co.zw,www.tumago.co.zw
CORS_ALLOWED_ORIGINS=https://tumago.co.zw,https://www.tumago.co.zw
PAYNOW_RESULT_URL=https://tumago.co.zw/api/v1/payment/callback/
```

Then restart the backend to pick up the new env:

```bash
cd ~/TumaGo/backend
docker compose -f docker-compose.prod.yml up -d
```

**Do NOT change** `SES_FROM_EMAIL` — email sending stays on `noreply@tishanyq.co.zw`.

---

## Verify Everything Works

Once all 5 steps are done:

| Test | How |
|------|-----|
| Frontend loads | Visit `https://tumago.co.zw` in a browser |
| API responds | Visit `https://tumago.co.zw/api/v1/health` |
| SSL valid | Browser shows padlock icon, no warnings |
| Android apps | Rebuild with `prod` flavor, test login + delivery request |

---

## Traffic Flow

```
Browser/App → tumago.co.zw (DNS → Elastic IP)
    → Nginx (host, :443 HTTPS)
        → /api/v1/*       → Go Gateway (:8080) → Django / services
        → /ws/*           → Go Gateway (:8080) → Location service (WebSocket)
        → /whatsapp/*     → Go Gateway (:8080) → WhatsApp service
        → /static/*       → Go Gateway (:8080) → Django static files
        → /* (all else)   → Next.js (:3000)    → Frontend
```
