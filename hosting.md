# AWS Multi-Client Hosting Guide

## Overview

One EC2 instance (t3.medium) hosting all client websites using nginx as a reverse proxy.

```
EC2 t3.medium
├── nginx (reverse proxy for ALL sites)
├── PostgreSQL (shared, one DB per client)
├── Redis (shared cache)
├── Static sites → nginx serves files directly
├── Car dealership backends → Docker containers
└── E-commerce backends → Docker containers
```

---

## Step 1: Launch the EC2 Instance

**AWS Console → EC2 → Launch Instance**

| Setting | Value | Why |
|---------|-------|-----|
| Name | client-hosting | Separate from TumaGo |
| AMI | Ubuntu 24.04 LTS | Stable, free |
| Instance type | t3.medium (2 vCPU, 4GB RAM) | Handles 10 sites + backends |
| Key pair | Create new or use existing .pem | For SSH access |
| Storage | 40GB gp3 | Room for Docker images, DBs, uploads |
| Region | af-south-1 (Cape Town) | Closest to Zimbabwe |

### Security Group Rules

| Type | Port | Source | Purpose |
|------|------|--------|---------|
| SSH | 22 | Your IP | Admin access |
| HTTP | 80 | 0.0.0.0/0 | Web traffic + certbot |
| HTTPS | 443 | 0.0.0.0/0 | Web traffic |

**Allocate an Elastic IP**: EC2 → Elastic IPs → Allocate → Associate to your instance. This gives you a fixed IP that doesn't change on reboot.

---

## Step 2: SSH In and Install Everything

```bash
ssh -i "your-key.pem" ubuntu@<your-elastic-ip>
```

```bash
# System updates
sudo apt update && sudo apt upgrade -y

# Install Docker
curl -fsSL https://get.docker.com | sudo sh
sudo usermod -aG docker ubuntu
newgrp docker

# Install Docker Compose
sudo apt install docker-compose-plugin -y

# Install nginx
sudo apt install nginx -y

# Install certbot for SSL
sudo apt install certbot python3-certbot-nginx -y

# Create swap (prevents OOM during builds)
sudo fallocate -l 2G /swapfile
sudo chmod 600 /swapfile
sudo mkswap /swapfile
sudo swapon /swapfile
echo '/swapfile none swap sw 0 0' | sudo tee -a /etc/fstab

# Create directory structure for all clients
sudo mkdir -p /var/www/sites
sudo chown -R ubuntu:ubuntu /var/www/sites
```

---

## Step 3: Set Up Shared PostgreSQL + Redis

Create `/home/ubuntu/infrastructure/docker-compose.yml`:

```yaml
services:
  db:
    image: postgres:15-alpine
    environment:
      POSTGRES_USER: admin
      POSTGRES_PASSWORD: <STRONG_PASSWORD_HERE>
    volumes:
      - postgres_data:/var/lib/postgresql/data
    ports:
      - "127.0.0.1:5432:5432"
    restart: always

  redis:
    image: redis:7-alpine
    command: redis-server --maxmemory 512mb --maxmemory-policy allkeys-lru
    volumes:
      - redis_data:/data
    ports:
      - "127.0.0.1:6379:6379"
    restart: always

volumes:
  postgres_data:
  redis_data:
```

Start the services:

```bash
cd /home/ubuntu/infrastructure
docker compose up -d
```

Create a separate database for each client that needs one:

```bash
docker exec -it infrastructure-db-1 psql -U admin -c "CREATE DATABASE cardealership1;"
docker exec -it infrastructure-db-1 psql -U admin -c "CREATE DATABASE cardealership2;"
docker exec -it infrastructure-db-1 psql -U admin -c "CREATE DATABASE cardealership3;"
docker exec -it infrastructure-db-1 psql -U admin -c "CREATE DATABASE ecommerce1;"
docker exec -it infrastructure-db-1 psql -U admin -c "CREATE DATABASE ecommerce2;"
```

---

## Step 4: Set Up S3 Bucket for Image Storage

**AWS Console → S3 → Create Bucket**

| Setting | Value |
|---------|-------|
| Name | client-uploads-youragency |
| Region | af-south-1 |
| Block public access | Uncheck "Block all" (images need to be public) |
| Bucket policy | Allow public read on /uploads/* |

Create an IAM user for your backends:

1. IAM → Users → Create → name: `hosting-s3-access`
2. Attach policy: `AmazonS3FullAccess` (or a scoped policy for your bucket)
3. Save the Access Key ID and Secret Access Key

Your backends use these credentials to upload car photos, product images, etc.

---

## Step 5: Deploy Static Sites

For each static site, put the built files in a directory:

```bash
# Example: client1 static site
mkdir -p /var/www/sites/client1
# Upload or git clone the built site here
```

Nginx serves these directly — no Docker needed.

---

## Step 6: Deploy Backend Sites with Docker

Each backend client gets their own folder and docker-compose:

```
/home/ubuntu/clients/
├── cardealership1/
│   ├── docker-compose.yml
│   └── .env
├── cardealership2/
│   ├── docker-compose.yml
│   └── .env
├── ecommerce1/
│   ├── docker-compose.yml
│   └── .env
└── ecommerce2/
    ├── docker-compose.yml
    └── .env
```

Example `/home/ubuntu/clients/cardealership1/docker-compose.yml`:

```yaml
services:
  web:
    image: youragency/cardealership1:latest
    build: .
    ports:
      - "127.0.0.1:3001:3000"
    environment:
      DATABASE_URL: postgresql://admin:<PASSWORD>@host.docker.internal:5432/cardealership1
      REDIS_URL: redis://host.docker.internal:6379
      AWS_ACCESS_KEY_ID: <key>
      AWS_SECRET_ACCESS_KEY: <secret>
      S3_BUCKET: client-uploads-youragency
    extra_hosts:
      - "host.docker.internal:host-gateway"
    restart: always
```

### Port Mapping Per Client

| Client | Internal Port | What |
|--------|--------------|------|
| Car Dealership 1 | 3001 | Next.js |
| Car Dealership 2 | 3002 | Next.js |
| Car Dealership 3 | 3003 | Next.js |
| E-commerce 1 | 4001 | Next.js + API |
| E-commerce 2 | 4002 | Next.js + API |

Start each client:

```bash
cd /home/ubuntu/clients/cardealership1
docker compose up -d --build
```

---

## Step 7: Configure Nginx for All Sites

Each client gets their own nginx config file.

### Static Site Config

`/etc/nginx/sites-available/client1`:

```nginx
server {
    listen 80;
    server_name client1.co.zw www.client1.co.zw;

    root /var/www/sites/client1;
    index index.html;

    location / {
        try_files $uri $uri/ /index.html;
    }
}
```

### Car Dealership Config

`/etc/nginx/sites-available/cardealership1`:

```nginx
server {
    listen 80;
    server_name carsharare.co.zw www.carsharare.co.zw;

    client_max_body_size 20M;

    location / {
        proxy_pass http://127.0.0.1:3001;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
    }
}
```

### Enable Each Site

```bash
sudo ln -s /etc/nginx/sites-available/client1 /etc/nginx/sites-enabled/
sudo ln -s /etc/nginx/sites-available/cardealership1 /etc/nginx/sites-enabled/
# ... repeat for all sites

sudo rm /etc/nginx/sites-enabled/default
sudo nginx -t && sudo systemctl reload nginx
```

---

## Step 8: SSL Certificates for All Domains

After DNS is pointed to your Elastic IP for all domains:

```bash
# Do all at once or one by one
sudo certbot --nginx -d client1.co.zw -d www.client1.co.zw
sudo certbot --nginx -d carsharare.co.zw -d www.carsharare.co.zw
# ... repeat for each domain

# Verify auto-renewal works
sudo certbot renew --dry-run
```

---

## Step 9: Automated Backups

Create `/home/ubuntu/scripts/backup.sh`:

```bash
#!/bin/bash
TIMESTAMP=$(date +%Y%m%d_%H%M)
BACKUP_DIR="/home/ubuntu/backups"
mkdir -p $BACKUP_DIR

# Dump all databases
docker exec infrastructure-db-1 pg_dumpall -U admin > "$BACKUP_DIR/all_dbs_$TIMESTAMP.sql"

# Keep only last 7 days
find $BACKUP_DIR -name "*.sql" -mtime +7 -delete
```

Set up the cron job:

```bash
chmod +x /home/ubuntu/scripts/backup.sh
crontab -e
# Add this line: 0 2 * * * /home/ubuntu/scripts/backup.sh
```

---

## Step 10: DNS for Each Client

For each client domain, on their DNS registrar (Cloudflare etc.):

| Type | Name | Value |
|------|------|-------|
| A | @ | your-elastic-ip |
| A | www | your-elastic-ip |

---

## Final Architecture

```
Client browser
    |
Cloudflare (free DNS + CDN + DDoS)
    |
EC2 t3.medium (Elastic IP)
    |
nginx (:80/:443)
    ├── client1.co.zw → /var/www/sites/client1/        (static)
    ├── client2.co.zw → /var/www/sites/client2/        (static)
    ├── client3.co.zw → /var/www/sites/client3/        (static)
    ├── client4.co.zw → /var/www/sites/client4/        (static)
    ├── client5.co.zw → /var/www/sites/client5/        (static)
    ├── cars1.co.zw   → localhost:3001                  (Docker)
    ├── cars2.co.zw   → localhost:3002                  (Docker)
    ├── cars3.co.zw   → localhost:3003                  (Docker)
    ├── shop1.co.zw   → localhost:4001                  (Docker)
    └── shop2.co.zw   → localhost:4002                  (Docker)

PostgreSQL (:5432) — shared, one DB per client
Redis (:6379) — shared cache
S3 — image storage for all clients
```

---

## Monthly Cost Summary

| Resource | Cost |
|----------|------|
| EC2 t3.medium | ~$40 |
| EBS 40GB gp3 | ~$4 |
| Elastic IP | $0 (attached) |
| S3 storage (~5GB images) | ~$1 |
| Data transfer | ~$5-15 |
| **Total** | **~$50-60/month** |

---

## Revenue vs Cost

| | Monthly |
|---|---|
| Client hosting revenue | $265 - $435 |
| Your AWS costs | $50 - $63 |
| **Profit (hosting alone)** | **$200 - $385** |
| **Annual recurring profit** | **$2,400 - $4,620** |

Plus $9,500 - $17,500 upfront from development.
