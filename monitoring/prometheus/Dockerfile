FROM alpine:3.20 AS tools
RUN apk add --no-cache curl
RUN curl -L https://github.com/a8m/envsubst/releases/download/v1.2.0/envsubst-Linux-x86_64 -o /usr/bin/envsubst && \
    chmod +x /usr/bin/envsubst

FROM prom/prometheus:v3.2.1

COPY --from=tools /usr/bin/envsubst /bin/envsubst
COPY prometheus.yaml /etc/prometheus/prometheus.template.yaml

COPY entrypoint.sh /entrypoint.sh
RUN chmod +x /entrypoint.sh

USER nobody

ENTRYPOINT ["/entrypoint.sh"]