#!/usr/bin/env bash
set -euo pipefail

SCENARIO=${1:?scenario name is required}
BASE_URL=${BASE_URL:-http://host.docker.internal:8080}
HOST=$(printf "%s" "${BASE_URL}" | sed -E 's#https?://([^:/]+).*#\1#')
PORT=$(printf "%s" "${BASE_URL}" | sed -nE 's#https?://[^:/]+:([0-9]+).*#\1#p')
PORT=${PORT:-8080}
THREADS=${THREADS:-32}
RAMP_UP=${RAMP_UP:-10}
DURATION=${DURATION:-60}
HEAVY_THREADS=${HEAVY_THREADS:-24}
ADMIN_THREADS=${ADMIN_THREADS:-3}
HEALTH_THREADS=${HEALTH_THREADS:-3}
JMETER_IMAGE=${JMETER_IMAGE:-justb4/jmeter:5.5}
JMETER_PLATFORM=${JMETER_PLATFORM:-linux/amd64}
TIMESTAMP=$(date +%Y%m%d-%H%M%S)
OUT_DIR="$(pwd)/load-tests/results/${SCENARIO}/${TIMESTAMP}"

mkdir -p "${OUT_DIR}"

docker run --rm \
  --platform "${JMETER_PLATFORM}" \
  -v "$(pwd)/load-tests:/load-tests" \
  -w /load-tests \
  "${JMETER_IMAGE}" \
  -n \
  -t "/load-tests/jmeter/${SCENARIO}.jmx" \
  -l "/load-tests/results/${SCENARIO}/${TIMESTAMP}/results.jtl" \
  -e \
  -o "/load-tests/results/${SCENARIO}/${TIMESTAMP}/html" \
  -Jhost="${HOST}" \
  -Jport="${PORT}" \
  -Jthreads="${THREADS}" \
  -JrampUp="${RAMP_UP}" \
  -Jduration="${DURATION}" \
  -JheavyThreads="${HEAVY_THREADS}" \
  -JadminThreads="${ADMIN_THREADS}" \
  -JhealthThreads="${HEALTH_THREADS}"

printf "JMeter results: %s\n" "${OUT_DIR}"
