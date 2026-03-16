#!/bin/bash
# =============================================================================
# TumaGo — Phase 1 EC2 Deployment Script
#
# Prerequisites:
#   - EC2 instance with Docker + Docker Compose installed
#   - AWS CLI configured (for ECR login)
#   - .env file with RDS_HOST, ELASTICACHE_HOST, ECR_REGISTRY, etc.
#
# Usage:
#   chmod +x deploy-ec2.sh
#   ./deploy-ec2.sh          # Deploy latest
#   ./deploy-ec2.sh rollback # Rollback to previous version
# =============================================================================

set -euo pipefail

COMPOSE_FILE="docker-compose.prod.yml"
APP_DIR="$HOME/tumago"
BACKUP_TAG="previous"

cd "$APP_DIR/backend"

# ── Check for .env file ────────────────────────────────────────────────
if [ ! -f .env ]; then
    echo "ERROR: .env file not found!"
    echo "Required variables: SECRET_KEY, RDS_HOST, ELASTICACHE_HOST, ECR_REGISTRY,"
    echo "  DJANGO_DB_PASS, LOCATION_DB_PASS, MATCHING_DB_PASS, NOTIFICATION_DB_PASS,"
    echo "  GOOGLE_MAPS_API_KEY, FIREBASE_CREDENTIALS_FILE, ALLOWED_HOSTS"
    exit 1
fi

source .env

# ── Rollback ────────────────────────────────────────────────────────────
if [ "${1:-}" = "rollback" ]; then
    echo "=== Rolling back to previous version ==="
    for svc in gateway location matching django notification; do
        echo "Restoring tumago-$svc:previous → tumago-$svc:latest"
        docker tag "$ECR_REGISTRY/tumago-$svc:$BACKUP_TAG" "$ECR_REGISTRY/tumago-$svc:latest" 2>/dev/null || \
            echo "WARNING: No previous image for tumago-$svc"
    done
    docker compose -f "$COMPOSE_FILE" up -d --remove-orphans
    echo "=== Rollback complete ==="
    exit 0
fi

# ── Login to ECR ────────────────────────────────────────────────────────
echo "=== Logging in to ECR ==="
aws ecr get-login-password --region af-south-1 | \
    docker login --username AWS --password-stdin "$ECR_REGISTRY"

# ── Tag current images as backup before pulling new ones ────────────────
echo "=== Backing up current images ==="
for svc in gateway location matching django notification; do
    docker tag "$ECR_REGISTRY/tumago-$svc:latest" "$ECR_REGISTRY/tumago-$svc:$BACKUP_TAG" 2>/dev/null || true
done

# ── Pull latest images ──────────────────────────────────────────────────
echo "=== Pulling latest images ==="
docker compose -f "$COMPOSE_FILE" pull

# ── Run Django migrations ────────────────────────────────────────────────
echo "=== Running Django migrations ==="
docker compose -f "$COMPOSE_FILE" run --rm web python manage.py migrate --noinput

# ── Start all services ──────────────────────────────────────────────────
echo "=== Starting services ==="
docker compose -f "$COMPOSE_FILE" up -d --remove-orphans

# ── Cleanup old images ──────────────────────────────────────────────────
docker image prune -f

# ── Health check ─────────────────────────────────────────────────────────
echo "=== Waiting for services to start ==="
sleep 5

echo ""
echo "=== TumaGo Phase 1 — Deployed ==="
echo ""
echo "Services:"
echo "  API Gateway:  http://$(curl -s http://169.254.169.254/latest/meta-data/public-ipv4 2>/dev/null || echo 'YOUR_EC2_IP'):80"
echo "  Health check: curl http://localhost/health"
echo ""
echo "Commands:"
echo "  docker compose -f $COMPOSE_FILE logs -f          # View logs"
echo "  docker compose -f $COMPOSE_FILE ps               # Service status"
echo "  docker compose -f $COMPOSE_FILE down              # Stop all"
echo "  ./deploy-ec2.sh rollback                          # Rollback to previous"
echo ""
