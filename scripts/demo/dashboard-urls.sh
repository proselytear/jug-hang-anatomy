#!/usr/bin/env bash
set -euo pipefail

BASE=${GRAFANA_URL:-http://localhost:3000}

printf "%s\n" \
  "${BASE}/d/hang-overview/hang-anatomy-overview" \
  "${BASE}/d/hang-three-queues/hang-anatomy-three-queues" \
  "${BASE}/d/hang-partner-resilience/hang-anatomy-partner-resilience" \
  "${BASE}/d/hang-health-admin/hang-anatomy-health-and-admin" \
  "${BASE}/d/hang-vt-experiment/hang-anatomy-virtual-threads-experiment" \
  "${BASE}/d/hang-final-reference/hang-anatomy-final-reference"
