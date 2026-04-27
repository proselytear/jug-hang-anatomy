#!/usr/bin/env bash
set -euo pipefail

MODE_FILE=${1:?mode file is required}
APP_MODE=${2:?app mode is required}
WIREMOCK_URL=${WIREMOCK_URL:-http://localhost:8089}
APP_URL=${APP_URL:-http://localhost:8080}

contains_mode() {
  local payload=$1
  local expected_mode=$2

  if command -v rg >/dev/null 2>&1; then
    printf "%s" "${payload}" | rg "\"mode\":\"${expected_mode}\"" >/dev/null
    return $?
  fi

  printf "%s" "${payload}" | grep -F "\"mode\":\"${expected_mode}\"" >/dev/null
}

wait_for_app_mode() {
  local expected_mode=$1
  local attempts=${2:-20}
  local sleep_seconds=${3:-1}

  for _ in $(seq 1 "${attempts}"); do
    local payload
    payload=$(curl -fsS "${APP_URL}/api/v1/demo/config")
    if contains_mode "${payload}" "${expected_mode}"; then
      return 0
    fi
    sleep "${sleep_seconds}"
  done

  printf "Failed to confirm app mode %s via %s/api/v1/demo/config\n" "${expected_mode}" "${APP_URL}" >&2
  return 1
}

curl -fsS -X POST \
  -H 'Content-Type: application/json' \
  --data-binary @"${MODE_FILE}" \
  "${WIREMOCK_URL}/__admin/mappings/import" >/dev/null

curl -fsS -X POST "${APP_URL}/api/v1/demo/mode/${APP_MODE}" >/dev/null
wait_for_app_mode "${APP_MODE}"

printf "WireMock mode applied: %s\n" "${APP_MODE}"
