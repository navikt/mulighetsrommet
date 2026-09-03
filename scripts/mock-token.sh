#!/usr/bin/env bash
set -euo pipefail

MOCK_BASE_URL="${MOCK_BASE_URL:-http://localhost:8081}"
ISSUER_ID="${ISSUER_ID:-azure}"
CLIENT_ID="${CLIENT_ID:-debugger}"
CLIENT_SECRET="${CLIENT_SECRET:-someSecret}"
SCOPE="${SCOPE:-openid somescope}"
STATE="${STATE:-local-dev-state}"
NONCE="${NONCE:-local-dev-nonce}"
REDIRECT_URI="${REDIRECT_URI:-${MOCK_BASE_URL}/${ISSUER_ID}/debugger/callback}"
USERNAME="${USERNAME:-local-dev-user}"
TOKEN_FILE="${TOKEN_FILE:-.local/mock-oauth-token-${ISSUER_ID}.json}"

urldecode() {
  local input="${1//+/ }"
  printf '%b' "${input//%/\\x}"
}

extract_json_string() {
  local key="$1"
  local file="$2"
  tr -d '\n' < "$file" | sed -n "s/.*\"${key}\"[[:space:]]*:[[:space:]]*\"\\([^\"]*\\)\".*/\\1/p"
}

extract_json_number() {
  local key="$1"
  local file="$2"
  tr -d '\n' < "$file" | sed -n "s/.*\"${key}\"[[:space:]]*:[[:space:]]*\\([0-9][0-9]*\\).*/\\1/p"
}

default_claims_for_issuer() {
  case "$ISSUER_ID" in
    azure)
      cat <<'EOF'
{"NAVident":"B123456","aud":["mulighetsrommet-api"],"oid":"0bab029e-e84e-4842-8a27-d153b29782cf","uti":"0bab029e-e84e-4842-8a27-d153b29782cf","groups":["52bb9196-b071-4cc7-9472-be4942d33c4b"]}
EOF
      ;;
    tokenx)
      cat <<'EOF'
{"pid":"11830348931","aud":["mulighetsrommet-api"]}
EOF
      ;;
    *)
      cat <<'EOF'
{"aud":["mulighetsrommet-api"]}
EOF
      ;;
  esac
}

CLAIMS_JSON="${CLAIMS_JSON:-$(default_claims_for_issuer)}"
AUTH_URL="${MOCK_BASE_URL}/${ISSUER_ID}/authorize"
TOKEN_URL="${MOCK_BASE_URL}/${ISSUER_ID}/token"

mkdir -p "$(dirname "$TOKEN_FILE")"

AUTH_URL_WITH_QUERY="$(
  curl -sS -G -o /dev/null -w '%{url_effective}' "$AUTH_URL" \
    --data-urlencode "client_id=${CLIENT_ID}" \
    --data-urlencode "scope=${SCOPE}" \
    --data-urlencode "response_type=code" \
    --data-urlencode "response_mode=query" \
    --data-urlencode "state=${STATE}" \
    --data-urlencode "nonce=${NONCE}" \
    --data-urlencode "redirect_uri=${REDIRECT_URI}"
)"

AUTH_BODY_FILE="$(mktemp)"
trap 'rm -f "$AUTH_BODY_FILE"' EXIT

LOCATION="$(
  curl -sS -o "$AUTH_BODY_FILE" -w '%{redirect_url}' -X POST "${AUTH_URL_WITH_QUERY}" \
    --data-urlencode "username=${USERNAME}" \
    --data-urlencode "claims=${CLAIMS_JSON}"
)"

CODE=""
if [ -n "$LOCATION" ]; then
  CODE_PART="$(printf '%s' "$LOCATION" | grep -Eo '[?#&]code=[^&#]*' | head -n1 || true)"
  if [ -n "$CODE_PART" ]; then
    CODE="$(urldecode "${CODE_PART#*=}")"
  fi
fi

if [ -z "$CODE" ]; then
  ERROR_BODY="$(cat "$AUTH_BODY_FILE" 2>/dev/null || true)"
  if [ -n "$ERROR_BODY" ]; then
    echo "Authorize-respons inneholdt ikke code. Body: ${ERROR_BODY}" >&2
  fi
  echo "Fant ikke authorization code i Location-header fra ${AUTH_URL}" >&2
  exit 1
fi

BASIC="$(printf '%s' "${CLIENT_ID}:${CLIENT_SECRET}" | base64 | tr -d '\n')"

curl -sS -X POST "$TOKEN_URL" \
  -H "Authorization: Basic ${BASIC}" \
  -H "Content-Type: application/x-www-form-urlencoded" \
  --data-urlencode "grant_type=authorization_code" \
  --data-urlencode "code=${CODE}" \
  --data-urlencode "scope=${SCOPE}" \
  --data-urlencode "redirect_uri=${REDIRECT_URI}" \
  > "$TOKEN_FILE"

ACCESS_TOKEN="$(extract_json_string "access_token" "$TOKEN_FILE")"
if [ -z "$ACCESS_TOKEN" ]; then
  echo "Mangler access_token i ${TOKEN_FILE}" >&2
  exit 1
fi

ISSUER_CLAIM="$(extract_json_string "iss" "$TOKEN_FILE")"
if [ -z "$ISSUER_CLAIM" ]; then
  ISSUER_CLAIM="unknown"
fi

EXPIRES_IN="$(extract_json_number "expires_in" "$TOKEN_FILE")"
if [ -z "$EXPIRES_IN" ]; then
  EXPIRES_IN="unknown"
fi

echo "Token skrevet til ${TOKEN_FILE} (issuer=${ISSUER_CLAIM}, expires_in=${EXPIRES_IN})"
