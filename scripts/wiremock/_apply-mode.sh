#!/usr/bin/env bash
set -euo pipefail

MODE_FILE=${1:?mode file is required}
APP_MODE=${2:?app mode is required}
WIREMOCK_URL=${WIREMOCK_URL:-http://localhost:8089}
APP_URL=${APP_URL:-http://localhost:8080}

curl -fsS -X POST \
  -H 'Content-Type: application/json' \
  --data-binary @"${MODE_FILE}" \
  "${WIREMOCK_URL}/__admin/mappings/import" >/dev/null

curl -fsS -X POST "${APP_URL}/api/v1/demo/mode/${APP_MODE}" >/dev/null || true

printf "WireMock mode applied: %s\n" "${APP_MODE}"
