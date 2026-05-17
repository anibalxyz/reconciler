#!/bin/sh
set -e

echo "Waiting for nginx to be ready..."
for i in $(seq 1 15); do
  wget -q --spider http://nginx/.well-known/acme-challenge/ 2>/dev/null && break
  sleep 2
done

if [ -d "/etc/letsencrypt/live/$DOMAIN" ]; then
  echo "Certificate exists, checking renewal..."
  certbot renew --webroot -w /var/www/certbot --quiet
  exit $?
fi

echo "Obtaining initial certificate..."
ARGS=""
[ "$CERTBOT_STAGING" = "true" ] && ARGS="--staging"
certbot certonly --webroot -w /var/www/certbot \
  --email "$CERTBOT_EMAIL" \
  -d "$DOMAIN" \
  --agree-tos --non-interactive \
  $ARGS
