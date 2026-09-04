#!/usr/bin/env bash
set -euo pipefail

MOCK_BASE_URL="${MOCK_BASE_URL:-http://localhost:8081}"
ISSUER_ID="${ISSUER_ID:-azure}"
CLIENT_ID="${MOCK_CLIENT_ID:-debugger}"
CLIENT_SECRET="${MOCK_CLIENT_SECRET:-someSecret}"
SCOPE="${MOCK_SCOPE:-openid somescope}"
STATE="${MOCK_STATE:-local-dev-state}"
NONCE="${MOCK_NONCE:-local-dev-nonce}"
REDIRECT_URI="${MOCK_BASE_URL}/${ISSUER_ID}/debugger/callback"
USERNAME="${USERNAME:-local-dev-user}"
TOKEN_FILE="${MOCK_TOKEN_FILE:-.local/mock-oauth-token-${ISSUER_ID}.json}"

urldecode() {
  printf '%s' "$1" | jq -Rr '
    gsub("\\+"; " ")
    | gsub("%(?<h>[0-9A-Fa-f]{2})"; "\\u00\(.h)")
    | "\"\(.)\""
    | fromjson
  '
}

if [ -n "${MOCK_CLAIMS_JSON:-}" ]; then
  CLAIMS_JSON="$(
    jq -cn \
      --argjson claims "${MOCK_CLAIMS_JSON}" \
      '$claims + (if has("aud") then {} else {"aud":["mulighetsrommet-api"]} end)'
  )"
else
  CLAIMS_JSON='{"aud":["mulighetsrommet-api"]}'
fi
AUTH_URL="${MOCK_BASE_URL}/${ISSUER_ID}/authorize"
TOKEN_URL="${MOCK_BASE_URL}/${ISSUER_ID}/token"

if ! command -v jq >/dev/null 2>&1; then
  echo "Mangler avhengighet: jq" >&2
  exit 1
fi

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

ACCESS_TOKEN="$(jq -er '.access_token' "$TOKEN_FILE")"

ISSUER_CLAIM="$(jq -r '.iss // "unknown"' "$TOKEN_FILE")"
if [ -z "$ISSUER_CLAIM" ]; then
  ISSUER_CLAIM="unknown"
fi

EXPIRES_IN="$(jq -r '.expires_in // "unknown"' "$TOKEN_FILE")"
if [ -z "$EXPIRES_IN" ]; then
  EXPIRES_IN="unknown"
fi

echo "Token skrevet til ${TOKEN_FILE} (issuer=${ISSUER_CLAIM}, expires_in=${EXPIRES_IN})"
