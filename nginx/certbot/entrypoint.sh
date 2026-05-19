#!/bin/sh
set -e

wait_for_nginx() {
  echo "Waiting for nginx to be ready..."
  for i in $(seq 1 15); do
    if wget -q -O /dev/null --no-check-certificate https://nginx/ 2>/dev/null; then
      echo "nginx is ready."
      return 0
    fi
    sleep 2
  done
  echo "ERROR: nginx did not become ready in time."
  return 1
}

is_self_signed() {
  local subject issuer raw_subject raw_issuer
  
  raw_subject=$(openssl x509 -in "$1" -noout -subject 2>/dev/null)
  raw_issuer=$(openssl x509 -in "$1" -noout -issuer 2>/dev/null)

  subject="${raw_subject#*=}"
  issuer="${raw_issuer#*=}"

  if [ "$subject" = "$issuer" ]; then
    return 0
  fi
  return 1
}

obtain_cert() {
  local args=""
  [ "$CERTBOT_STAGING" = "true" ] && args="--staging"
  certbot certonly --webroot -w /var/www/certbot \
    --email "$CERTBOT_EMAIL" \
    -d "$DOMAIN" \
    --agree-tos --non-interactive \
    $args
}

renew_cert() {
  certbot renew --non-interactive
}

# --- Wait for nginx before doing anything ---
wait_for_nginx || exit 1

# --- Certificate state machine ---
CERT_DIR="/etc/letsencrypt/live/$DOMAIN"
CERT_FILE="$CERT_DIR/fullchain.pem"

if [ -f "$CERT_FILE" ]; then
  if is_self_signed "$CERT_FILE"; then
    echo "Self-signed certificate detected. Preparing upgrade..."

    mv "$CERT_DIR" "${CERT_DIR}.bak"

    if obtain_cert; then
      echo "Successfully upgraded to Let's Encrypt certificate."
      rm -rf "${CERT_DIR}.bak"
    else
      echo "WARNING: Certbot failed. Restoring self-signed fallback..."
      rm -rf "$CERT_DIR"
      mv "${CERT_DIR}.bak" "$CERT_DIR"
    fi
  else
    echo "Existing Let's Encrypt certificate found. Checking renewal..."
    renew_cert
  fi
else
  echo "No certificate found. Obtaining..."
  obtain_cert
fi
