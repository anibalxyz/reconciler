#!/bin/sh
set -e

echo "==> Resolving env vars in prometheus.yaml..."
envsubst < /etc/prometheus/prometheus.template.yaml > /tmp/prometheus.yml

echo "==> Starting Prometheus..."
exec /bin/prometheus \
    --config.file=/tmp/prometheus.yml \
    --storage.tsdb.path=/prometheus \
    --web.console.libraries=/usr/share/prometheus/console_libraries \
    --web.console.templates=/usr/share/prometheus/consoles \
    "$@"