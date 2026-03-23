#!/usr/bin/env bash
set -euo pipefail

BASE_URL="${BASE_URL:-http://localhost:8080/api/v1}"
PARALLEL_USERS="${PARALLEL_USERS:-10}"

EMAIL="${EMAIL:-p4_concurrency_user@example.com}"
USERNAME="${USERNAME:-p4_concurrency_user}"
FULL_NAME="${FULL_NAME:-P4 Concurrency User}"
PASSWORD="${PASSWORD:-StrongPass!123}"

seed_user_if_needed() {
  local register_payload
  register_payload=$(cat <<JSON
{"email":"${EMAIL}","username":"${USERNAME}","fullName":"${FULL_NAME}","password":"${PASSWORD}"}
JSON
)

  curl -sS -X POST "${BASE_URL}/auth/register" \
    -H "Content-Type: application/json" \
    -d "${register_payload}" >/dev/null || true
}

login_once() {
  local idx="$1"
  local login_payload

  login_payload=$(cat <<JSON
{"email":"${EMAIL}","password":"${PASSWORD}"}
JSON
)

  local login_response
  login_response=$(curl -sS -X POST "${BASE_URL}/auth/login" \
    -H "Content-Type: application/json" \
    -d "${login_payload}")

  if echo "${login_response}" | grep -q '"token"'; then
    echo "user-${idx}: login ok"
  else
    echo "user-${idx}: login failed"
    echo "response=${login_response}"
    return 1
  fi
}

export -f login_once
export BASE_URL EMAIL PASSWORD

seed_user_if_needed

echo "Running ${PARALLEL_USERS}-user concurrent auth smoke test against ${BASE_URL}"
seq 1 "${PARALLEL_USERS}" | xargs -P"${PARALLEL_USERS}" -I{} bash -c 'login_once "$@"' _ {}

echo "Concurrent auth smoke test completed"
