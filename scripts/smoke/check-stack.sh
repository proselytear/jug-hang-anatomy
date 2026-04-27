#!/usr/bin/env bash
set -euo pipefail

APP_URL=${APP_URL:-http://localhost:8080}
GRAFANA_URL=${GRAFANA_URL:-http://localhost:3000}
WIREMOCK_URL=${WIREMOCK_URL:-http://localhost:8089}
LOKI_URL=${LOKI_URL:-http://localhost:3100}
MIMIR_URL=${MIMIR_URL:-http://localhost:9009}
TEMPO_URL=${TEMPO_URL:-http://localhost:3200}

contains_pattern() {
  local payload=$1
  local pattern=$2

  if command -v rg >/dev/null 2>&1; then
    printf "%s" "${payload}" | rg "${pattern}" >/dev/null
    return $?
  fi

  printf "%s" "${payload}" | grep -E "${pattern}" >/dev/null
}

wait_for() {
  local url=$1
  local label=$2
  local attempts=${3:-30}
  local sleep_seconds=${4:-2}

  for _ in $(seq 1 "${attempts}"); do
    if curl -fsS "${url}" >/dev/null 2>/dev/null; then
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
prometheus_payload=$(curl -fsS "${APP_URL}/actuator/prometheus")
contains_pattern "${prometheus_payload}" "http_server_requests|tomcat_threads_busy|hikaricp_connections_active"

curl -fsS "${APP_URL}/api/v1/dashboard/00000000-0000-0000-0000-000000000001" >/dev/null

printf "Smoke checks passed.\n"
