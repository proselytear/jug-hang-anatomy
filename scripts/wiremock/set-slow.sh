#!/usr/bin/env bash
set -euo pipefail

"$(dirname "$0")/_apply-mode.sh" \
  "$(pwd)/infrastructure/wiremock/imports/mode-slow-import.json" \
  "slow-partner"
