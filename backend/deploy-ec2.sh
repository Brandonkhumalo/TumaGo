#!/bin/bash
# =============================================================================
# TumaGo — EC2 t3.small deployment script
# Run on a fresh Ubuntu 22.04 / Amazon Linux 2023 EC2 instance
#
# Usage:
#   chmod +x deploy-ec2.sh
#   ./deploy-ec2.sh
# =============================================================================

set -euo pipefail

echo "=== TumaGo EC2 Setup ==="

# ── Install Docker ──────────────────────────────────────────────────────
if ! command -v docker &> /dev/null; then
    echo "Installing Docker..."
    curl -fsSL https://get.docker.com | sh
    sudo usermod -aG docker "$USER"
    echo "Docker installed. Log out and back in, then re-run this script."
    exit 0
fi

# ── Install Docker Compose plugin ──────────────────────────────────────
if ! docker compose version &> /dev/null; then
    echo "Installing Docker Compose plugin..."
    sudo apt-get update && sudo apt-get install -y docker-compose-plugin 2>/dev/null || \
    sudo yum install -y docker-compose-plugin 2>/dev/null || true
fi

# ── Clone repo (skip if already cloned) ────────────────────────────────
REPO_DIR="$HOME/TumaGo"
if [ ! -d "$REPO_DIR" ]; then
    echo "Cloning repository..."
    git clone https://github.com/YOUR_USERNAME/TumaGo.git "$REPO_DIR"
fi

cd "$REPO_DIR/backend"

# ── Check for .env file ────────────────────────────────────────────────
if [ ! -f .env ]; then
    echo ""
    echo "ERROR: .env file not found!"
    echo "Copy and edit the example:"
    echo "  cp .env.example .env"
    echo "  nano .env"
    echo ""
    exit 1
fi

# ── Run Django migrations ──────────────────────────────────────────────
echo "Building and starting services..."
docker compose -f docker-compose.t3small.yml build

echo "Starting database first..."
docker compose -f docker-compose.t3small.yml up -d db redis pgbouncer
echo "Waiting for database to be ready..."
sleep 10

echo "Running Django migrations..."
docker compose -f docker-compose.t3small.yml run --rm web python manage.py migrate

echo "Starting all services..."
docker compose -f docker-compose.t3small.yml up -d

echo ""
echo "=== TumaGo is running! ==="
echo ""
echo "Services:"
echo "  API Gateway:    http://$(curl -s http://169.254.169.254/latest/meta-data/public-ipv4 2>/dev/null || echo 'YOUR_EC2_IP'):80"
echo "  Health check:   curl http://localhost/health"
echo ""
echo "Useful commands:"
echo "  docker compose -f docker-compose.t3small.yml logs -f        # View logs"
echo "  docker compose -f docker-compose.t3small.yml ps             # Service status"
echo "  docker compose -f docker-compose.t3small.yml down           # Stop all"
echo "  docker compose -f docker-compose.t3small.yml restart web    # Restart Django"
echo ""
