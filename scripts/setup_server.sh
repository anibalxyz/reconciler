#!/usr/bin/env bash
set -euo pipefail
export PATH="/usr/sbin:/sbin:$PATH"
PROJECT_DIR="${HOME}/reconciler"

# ──────────────────────────────────────────────
# Functions
# ──────────────────────────────────────────────

install_docker() {
  . /etc/os-release

  if [ "$ID" != "ubuntu" ] && [ "$ID" != "debian" ]; then
    echo "Error: Unsupported distribution: ${ID}"
    exit 1
  fi

  DOCKER_VERSION="29.5.0"
  COMPOSE_VERSION="5.1.3"

  apt-get update -qq
  apt-get install -y -qq ca-certificates curl python3 python3-pip

  install -m 0755 -d /etc/apt/keyrings
  curl -fsSL "https://download.docker.com/linux/${ID}/gpg" -o /etc/apt/keyrings/docker.asc
  chmod a+r /etc/apt/keyrings/docker.asc

  tee /etc/apt/sources.list.d/docker.sources > /dev/null <<EOF
Types: deb
URIs: https://download.docker.com/linux/${ID}
Suites: ${VERSION_CODENAME}
Components: stable
Architectures: $(dpkg --print-architecture)
Signed-By: /etc/apt/keyrings/docker.asc
EOF

  apt-get update -qq

  DOCKER_VERSION_STRING="5:${DOCKER_VERSION}-1~${ID}.${VERSION_ID}~${VERSION_CODENAME}"
  COMPOSE_VERSION_STRING="${COMPOSE_VERSION}-1~${ID}.${VERSION_ID}~${VERSION_CODENAME}"

  apt-get install -y \
    docker-ce="${DOCKER_VERSION_STRING}" \
    docker-ce-cli="${DOCKER_VERSION_STRING}" \
    containerd.io \
    docker-buildx-plugin \
    docker-compose-plugin="${COMPOSE_VERSION_STRING}"

  systemctl enable --now docker
}

configure_ufw() {
  apt-get install -y -qq ufw

  ufw default deny incoming
  ufw default allow outgoing

  ufw allow 22/tcp
  ufw allow 80/tcp
  ufw allow 443/tcp

  ufw --force enable
}

configure_fail2ban() {
  apt-get install -y -qq fail2ban

  tee /etc/fail2ban/jail.local > /dev/null <<'EOF'
[DEFAULT]
ignoreip = 127.0.0.1/8 ::1
bantime = 18000
findtime = 600
maxretry = 3

[sshd]
enabled = true
port = ssh
logpath = %(sshd_log)s
EOF

  systemctl enable --now fail2ban
}

fetch_deploy_assets() {
  echo "==> Downloading deploy assets..."
  mkdir -p "$PROJECT_DIR"
  curl -fsSL "https://github.com/anibalxyz/reconciler/releases/latest/download/release-assets.tar.gz" \
    | tar xz -C "$PROJECT_DIR"

  cd "$PROJECT_DIR"
  pip3 install -q --break-system-packages --root-user-action=ignore --force-reinstall "$PROJECT_DIR"/cli/dist/reconciler_cli-*.whl
  cli --install-completion 2>/dev/null || true
}

setup_env_files() {
  local domain="$1"
  local email="$2"

  if [ ! -f "$PROJECT_DIR/frontend/.env.prod" ]; then
    echo "==> Injecting DOMAIN and CERTBOT_EMAIL into frontend/.env.prod..."
    cp "$PROJECT_DIR/frontend/.env.prod.example" "$PROJECT_DIR/frontend/.env.prod"
    sed -i "s/^DOMAIN=.*/DOMAIN=${domain}/" "$PROJECT_DIR/frontend/.env.prod"
    sed -i "s/^CERTBOT_EMAIL=.*/CERTBOT_EMAIL=${email}/" "$PROJECT_DIR/frontend/.env.prod"
  else
    echo "  Skipped frontend/.env.prod creation: file already exists."
  fi

  if [ ! -f "$PROJECT_DIR/backend/.env.prod" ]; then
    echo "==> Creating backend/.env.prod from template..."
    cp "$PROJECT_DIR/backend/.env.prod.example" "$PROJECT_DIR/backend/.env.prod"
  else
    echo "  Skipped backend/.env.prod creation: file already exists."
  fi
}

show_final_message() {
  echo -e "\n\e[1;32m══> Install complete! ════════════════════════════════════════\e[0m"
  echo ""
  echo "Review and edit remaining variables in:"
  echo "  $PROJECT_DIR/frontend/.env.prod"
  echo "  $PROJECT_DIR/backend/.env.prod"
  echo ""
  echo "Check env vars like LIMIT_REQ_RATE, API_PORT, JWT_ISSUER,"
  echo "JWT_*_EXPIRATION_TIME_* and adjust to your needs."
  echo ""
  echo "Note: Secrets will be generated during deployment if left as CHANGEME (recommended)."
  echo ""
  echo "Once ready, run:"
  echo -e "  \e[1;36msudo bash ./setup_server.sh deploy\e[0m"
  echo -e "\e[1;32m══════════════════════════════════════════════════════════════\e[0m\n"
}

setup_cron() {
  local cron_certbot="/etc/cron.d/reconciler-certbot"
  local cron_prune="/etc/cron.d/reconciler-docker-prune"

  # TODO: check 'compose up' does not leave orphan containers
  echo "==> Setting up certbot renewal cron..."
  cat > "$cron_certbot" <<CRON
PATH=/usr/local/bin:/usr/sbin:/sbin:/usr/bin:/bin
0 */12 * * * root cd ${PROJECT_DIR} && cli compose up certbot --no-detach && docker kill -s HUP reconciler-prod-nginx
CRON
  chmod 644 "$cron_certbot"
  echo "  Created $cron_certbot"
  
  echo "==> Setting up docker prune cron..."
  cat > "$cron_prune" <<CRON
PATH=/usr/local/bin:/usr/sbin:/sbin:/usr/bin:/bin
0 3 * * 0 root cli resource prune all
CRON
  chmod 644 "$cron_prune"
  echo "  Created $cron_prune"
}

deploy() {
  cd "$PROJECT_DIR"

  # Ensure .env.prod files exist (create from example if missing)
  ensure_env_file() {
    local src="${1}"
    local dst="${2}"
    if [ ! -f "$dst" ]; then
      cp "$src" "$dst"
      echo "  Created $dst from template"
    fi
  }

  # Process a secret: generate only if empty or CHANGEME, otherwise keep existing
  process_secret() {
    local key="${1}"
    local length="${2}"
    local env_file="${3}"

    local current_value=$(grep "^${key}=" "$env_file" 2>/dev/null | cut -d'=' -f2-)

    if [ -z "$current_value" ] || [ "$current_value" = "CHANGEME" ]; then
      local new_value=$(openssl rand -hex "$length")
      sed -i "s|^${key}=.*|${key}=${new_value}|" "$env_file"
      echo "  Generated new ${key}"
    else
      echo "  Kept existing ${key}"
    fi
  }

  echo "==> Ensuring .env.prod files exist..."
  ensure_env_file backend/.env.prod.example backend/.env.prod
  ensure_env_file frontend/.env.prod.example frontend/.env.prod

  echo "==> Processing secrets..."
  process_secret "DB_PASSWORD" 64 backend/.env.prod
  process_secret "JWT_SECRET" 32 backend/.env.prod
  process_secret "GRAFANA_ADMIN_PASSWORD" 16 backend/.env.prod

  echo "==> Setting environment to prod..."
  cli set env prod

  echo "==> Starting production services..."
  cli compose up

  echo "==> Waiting for Certbot to finish initial certificate generation..."
  echo "    (This can take up to 1-2 minutes while validating with Let's Encrypt. Please wait...)"
  docker wait reconciler-prod-certbot >/dev/null || true

  echo "==> Reloading Nginx to apply certificates..."
  docker kill -s HUP reconciler-prod-nginx > /dev/null
}

main() {
  if [ "$(id -u)" -ne 0 ]; then
    echo "Error: This script must be run as root (use sudo)."
    exit 1
  fi

  case "${1:-}" in
    install)
      DOMAIN="${2:-}"
      EMAIL="${3:-}"

      if [ -z "$DOMAIN" ] || [ -z "$EMAIL" ]; then
        echo "Usage: sudo ./setup_server.sh install <domain> <email>"
        echo "  domain — domain (e.g. reconciler.duckdns.org)"
        echo "  email  — Let's Encrypt registration email"
        exit 1
      fi

      echo "==> Starting server installation for ${DOMAIN}"
      install_docker
      configure_ufw
      configure_fail2ban
      fetch_deploy_assets

      setup_env_files "$DOMAIN" "$EMAIL"
      setup_cron

      show_final_message
      ;;
    deploy)
      deploy
      ;;
    *)
      echo "Usage: sudo ./setup_server.sh install|deploy"
      echo ""
      echo "  install <domain> <email>"
      echo "    Bootstrap the server: install Docker, UFW, fail2ban,"
      echo "    download deploy assets, and install the CLI."
      echo ""
      echo "  deploy"
      echo "    Configure secrets and start production services."
      echo "    backend/.env.prod and frontend/.env.prod must exist."
      exit 1
      ;;
  esac
}

if [[ "${BASH_SOURCE[0]}" == "${0}" ]]; then
  main "$@"
fi
