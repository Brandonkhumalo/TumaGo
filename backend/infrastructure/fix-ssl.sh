#!/bin/bash
# ============================================================================
# TumaGo — SSL Recovery Script
#
# Run this on the EC2 server when tumago.co.zw becomes unreachable after a
# deploy or reboot. It restores the nginx config and reinstalls Let's Encrypt
# certificates if needed.
#
# Usage:  ssh into the server, then:
#   cd ~/TumaGo && bash backend/infrastructure/fix-ssl.sh
# ============================================================================

set -e

DOMAIN="tumago.co.zw"
NGINX_SITE="/etc/nginx/sites-available/tumago"
NGINX_ENABLED="/etc/nginx/sites-enabled/tumago"
CERT_DIR="/etc/letsencrypt/live/$DOMAIN"
REPO_SSL_CONF="$HOME/TumaGo/backend/infrastructure/nginx/tumago-ssl.conf"
REPO_HTTP_CONF="$HOME/TumaGo/backend/infrastructure/nginx/tumago.conf"

echo "============================================"
echo "  TumaGo SSL Recovery"
echo "============================================"
echo ""

# Step 1: Ensure nginx is installed
if ! command -v nginx &> /dev/null; then
    echo "[1/6] Installing nginx..."
    sudo apt-get update -qq && sudo apt-get install -y nginx
else
    echo "[1/6] nginx is installed"
fi

# Step 2: Ensure certbot is installed
if ! command -v certbot &> /dev/null; then
    echo "[2/6] Installing certbot..."
    sudo apt-get update -qq && sudo apt-get install -y certbot python3-certbot-nginx
else
    echo "[2/6] certbot is installed"
fi

# Step 3: Create certbot webroot directory
sudo mkdir -p /var/www/certbot
echo "[3/6] Certbot webroot ready"

# Step 4: Check if SSL certs exist
if [ -d "$CERT_DIR" ] && [ -f "$CERT_DIR/fullchain.pem" ]; then
    echo "[4/6] SSL certificates exist — using SSL config"
    sudo cp "$REPO_SSL_CONF" "$NGINX_SITE"
else
    echo "[4/6] No SSL certificates found — installing fresh certs..."

    # First deploy HTTP-only config so certbot can verify the domain
    sudo cp "$REPO_HTTP_CONF" "$NGINX_SITE"

    # Ensure the site is enabled
    if [ ! -L "$NGINX_ENABLED" ]; then
        sudo ln -sf "$NGINX_SITE" "$NGINX_ENABLED"
    fi

    # Remove default site if it conflicts
    sudo rm -f /etc/nginx/sites-enabled/default

    # Test and reload nginx with HTTP config
    sudo nginx -t && sudo systemctl reload nginx

    # Request SSL certificate from Let's Encrypt
    echo ""
    echo "  Requesting SSL certificate for $DOMAIN..."
    echo ""
    sudo certbot certonly \
        --webroot \
        -w /var/www/certbot \
        -d "$DOMAIN" \
        -d "www.$DOMAIN" \
        --non-interactive \
        --agree-tos \
        --email brandon@tishanyq.co.zw \
        --no-eff-email

    # Now switch to SSL config
    sudo cp "$REPO_SSL_CONF" "$NGINX_SITE"
    echo "[4/6] SSL certificates installed"
fi

# Step 5: Ensure site is enabled and default is removed
if [ ! -L "$NGINX_ENABLED" ]; then
    sudo ln -sf "$NGINX_SITE" "$NGINX_ENABLED"
fi
sudo rm -f /etc/nginx/sites-enabled/default
echo "[5/6] Site enabled"

# Step 6: Test and reload nginx
echo "[6/6] Testing and reloading nginx..."
sudo nginx -t && sudo systemctl reload nginx

echo ""
echo "============================================"
echo "  Done! https://$DOMAIN should be live"
echo "============================================"
echo ""
echo "  Verify:  curl -I https://$DOMAIN"
echo ""
echo "  If certs expire, renew with:"
echo "    sudo certbot renew"
echo ""
