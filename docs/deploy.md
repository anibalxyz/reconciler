# Deploy

## Environment Setup

Define these variables in your terminal before running the following blocks:

```bash
DOMAIN="reconciler.domain.com"
EMAIL="admin@example.com"
```

## Prerequisites

* **OS**: Ubuntu or Debian (penultimate stable release recommended)
* **Ports**: 22, 80, and 443 must be reachable
* **Domain**: DNS A record pointing to the server's public IP
* **Email**: valid address for Let's Encrypt registration (renewal notices)

## Automated Setup

### 1. Download and run the bootstrap script

```bash
curl -fsSL https://raw.githubusercontent.com/anibalxyz/reconciler/main/scripts/setup_server.sh -o setup_server.sh
sudo bash setup_server.sh install $DOMAIN $EMAIL

```

This installs Docker, UFW, fail2ban, downloads the latest release assets (compose files, CLI wheel, monitoring configs), and sets up the env file templates.

### 2. Configure environment variables

Review and adjust the generated files:

* `frontend/.env.prod`: `DOMAIN`, `CERTBOT_EMAIL`, rate limits, TTL
* `backend/.env.prod`: log level, timezone, CORS origins, JWT issuer, auth cookie settings

Secrets (`DB_PASSWORD`, `JWT_SECRET`, `GRAFANA_ADMIN_PASSWORD`) can stay as `CHANGEME` if you want them to be auto-generated during deploy.

### 3. Deploy

```bash
sudo bash setup_server.sh deploy
```

This generates any missing secrets, starts all production services, waits for Certbot to obtain the initial Let's Encrypt certificate, and reloads Nginx.

## Verification

Once deployed, confirm everything is running:

```bash
# HTTPS is working and redirects from HTTP
curl -I https://$DOMAIN

# API is responsive (should return 401 without auth, but confirms backend is up)
curl https://$DOMAIN/api/users

# Services are up
cli resource list containers

# Certificate is valid and not self-signed
openssl s_client -connect $DOMAIN:443 -servername $DOMAIN </dev/null 2>/dev/null | openssl x509 -noout -subject -issuer -dates
```

## Automated Maintenance

System cron files are automatically configured under `/etc/cron.d/` during setup:

* **SSL Renewal (`reconciler-certbot`)**: Runs every 12 hours. It automatically checks and renews the Let's Encrypt certificate, reloading Nginx gracefully with zero downtime if changes are applied.
* **Disk Cleanup (`reconciler-docker-prune`)**: Runs weekly (Sundays at 3:00 AM). It safely prunes dangling images and old stopped containers to prevent the VPS from running out of disk space.
