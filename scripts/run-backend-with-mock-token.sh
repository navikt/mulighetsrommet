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

ACCESS_TOKEN="$(
  python3 - <<'PY' "$TOKEN_FILE"
import json
import sys
from pathlib import Path

token_file = Path(sys.argv[1])
payload = json.loads(token_file.read_text())
token = payload.get("access_token")
if not token:
    raise SystemExit("Mangler access_token i tokenfil")
print(token)
PY
)"

exec env VITE_MULIGHETSROMMET_API_AUTH_TOKEN="${ACCESS_TOKEN}" "$@"
