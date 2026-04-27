#!/usr/bin/env bash
set -euo pipefail

APP_URL=${APP_URL:-http://localhost:8080}
GRAFANA_URL=${GRAFANA_URL:-http://localhost:3000}
WIREMOCK_URL=${WIREMOCK_URL:-http://localhost:8089}
LOKI_URL=${LOKI_URL:-http://localhost:3100}
MIMIR_URL=${MIMIR_URL:-http://localhost:9009}
TEMPO_URL=${TEMPO_URL:-http://localhost:3200}

wait_for() {
  local url=$1
  local label=$2
  local attempts=${3:-30}
  local sleep_seconds=${4:-2}

  for _ in $(seq 1 "${attempts}"); do
    if curl -fsS "${url}" >/dev/null; then
      return 0
    fi
    sleep "${sleep_seconds}"
  done

  printf "Smoke check failed waiting for %s: %s\n" "${label}" "${url}" >&2
  return 1
}

wait_for "${APP_URL}/actuator/health" "app"
wait_for "${APP_URL}/api/v1/demo/config" "demo config"
wait_for "${GRAFANA_URL}/api/health" "grafana"
wait_for "${WIREMOCK_URL}/__admin/health" "wiremock"
wait_for "${LOKI_URL}/ready" "loki"
wait_for "${MIMIR_URL}/metrics" "mimir"
wait_for "${TEMPO_URL}/metrics" "tempo"
curl -fsS "${APP_URL}/actuator/prometheus" | rg "http_server_requests|tomcat_threads_busy|hikaricp_connections_active" >/dev/null

curl -fsS "${APP_URL}/api/v1/dashboard/00000000-0000-0000-0000-000000000001" >/dev/null

printf "Smoke checks passed.\n"
