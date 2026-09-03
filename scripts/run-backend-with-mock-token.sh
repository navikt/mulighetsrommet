#!/usr/bin/env bash
set -euo pipefail

if [ "${1:-}" = "--" ]; then
  shift
fi

if [ "$#" -eq 0 ]; then
  echo "Bruk: run-backend-with-mock-token.sh -- <kommando>" >&2
  exit 1
fi

SCRIPT_DIR="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")" && pwd)"
REPO_ROOT="$(cd -- "${SCRIPT_DIR}/.." && pwd)"
MOCK_ISSUER_ID="${MOCK_ISSUER_ID:-azure}"

export ISSUER_ID="${ISSUER_ID:-$MOCK_ISSUER_ID}"
export TOKEN_FILE="${TOKEN_FILE:-${REPO_ROOT}/.local/mock-oauth-token-${ISSUER_ID}.json}"

"${SCRIPT_DIR}/mock-token.sh"

if ! command -v jq >/dev/null 2>&1; then
  echo "Mangler avhengighet: jq" >&2
  exit 1
fi

ACCESS_TOKEN="$(jq -er '.access_token' "$TOKEN_FILE")"

exec env VITE_MULIGHETSROMMET_API_AUTH_TOKEN="${ACCESS_TOKEN}" "$@"
