#!/usr/bin/env bash
set -euo pipefail

BASE_URL="${BASE_URL:-http://localhost:8081/api/v1}"
PARALLEL_USERS="${PARALLEL_USERS:-10}"

EMAIL="${EMAIL:-p4_e2e_user@example.com}"
USERNAME="${USERNAME:-p4_e2e_user}"
FULL_NAME="${FULL_NAME:-P4 E2E User}"
PASSWORD="${PASSWORD:-StrongPass!123}"

CURRENT_USER_ID=""

json_field() {
  local expr="$1"
  node -e "let d='';process.stdin.on('data',c=>d+=c);process.stdin.on('end',()=>{const j=JSON.parse(d);const v=(${expr});if(v===undefined||v===null){process.exit(2)};process.stdout.write(String(v));});"
}

register_user() {
  local register_payload
  register_payload=$(cat <<JSON
{"email":"${EMAIL}","username":"${USERNAME}","fullName":"${FULL_NAME}","password":"${PASSWORD}"}
JSON
)

  curl -sS -X POST "${BASE_URL}/auth/register" \
    -H "Content-Type: application/json" \
    -d "${register_payload}" >/dev/null || true
}

login_and_capture_identity() {
  local login_payload
  login_payload=$(cat <<JSON
{"email":"${EMAIL}","password":"${PASSWORD}"}
JSON
)

  local login_response
  login_response=$(curl -sS -X POST "${BASE_URL}/auth/login" \
    -H "Content-Type: application/json" \
    -d "${login_payload}")

  if ! echo "${login_response}" | grep -q '"token"'; then
    echo "Login failed: ${login_response}"
    exit 1
  fi

  CURRENT_USER_ID=$(echo "${login_response}" | json_field 'j.user.id')
  echo "${login_response}" | json_field 'j.token'
}

create_project_and_stage() {
  local token="$1"
  local project_payload
  project_payload=$(cat <<JSON
{"name":"P4 Concurrency Project $(date +%s)","description":"Concurrency verification run","generateTasks":false}
JSON
)

  local project_response
  project_response=$(curl -sS -X POST "${BASE_URL}/projects" \
    -H "Authorization: Bearer ${token}" \
    -H "Content-Type: application/json" \
    -d "${project_payload}")

  local project_id board_id stage_id
  project_id=$(echo "${project_response}" | json_field 'j.id')
  board_id=$(echo "${project_response}" | json_field 'j.board_id')
  stage_id=$(echo "${project_response}" | json_field 'j.columns && j.columns[0] ? j.columns[0].id : undefined')

  if [[ -z "${project_id}" || -z "${board_id}" || -z "${stage_id}" ]]; then
    echo "Project/bootstrap parsing failed: ${project_response}"
    exit 1
  fi

  echo "${project_id}|${board_id}|${stage_id}"
}

create_card_once() {
  local idx="$1"
  local token="$2"
  local stage_id="$3"

  local payload
  payload=$(cat <<JSON
{"title":"Concurrent Card ${idx}","description":"Created in parallel","priority":"MEDIUM"}
JSON
)

  local response
  response=$(curl -sS -w "\n%{http_code}" -X POST "${BASE_URL}/boards/stages/${stage_id}/cards" \
    -H "Authorization: Bearer ${token}" \
    -H "Content-Type: application/json" \
    -d "${payload}")

  local body status
  body=$(echo "${response}" | sed '$d')
  status=$(echo "${response}" | tail -n1)

  if [[ "${status}" == "200" ]] && echo "${body}" | grep -q '"id"'; then
    echo "ok"
  else
    echo "fail:${status}"
  fi
}

create_meeting_and_summary() {
  local token="$1"
  local project_id="$2"

  local meeting_payload
  local meeting_date
  meeting_date=$(date -d 'tomorrow' +%F)
  meeting_payload=$(cat <<JSON
{"title":"Concurrency Meeting","description":"Parallel approvals","meetingDate":"${meeting_date}","meetingTime":"10:00:00","projectId":"${project_id}","platform":"Zoom","meetingLink":"https://example.com/meet"}
JSON
)

  local meeting_response
  meeting_response=$(curl -sS -w "\n%{http_code}" -X POST "${BASE_URL}/meetings" \
    -H "Authorization: Bearer ${token}" \
    -H "Content-Type: application/json" \
    -d "${meeting_payload}")

  local meeting_body meeting_status
  meeting_body=$(echo "${meeting_response}" | sed '$d')
  meeting_status=$(echo "${meeting_response}" | tail -n1)
  if [[ "${meeting_status}" != "201" ]]; then
    echo "Create meeting failed: ${meeting_body}"
    exit 1
  fi

  local meeting_id
  meeting_id=$(echo "${meeting_body}" | json_field 'j.id')

  local add_member_payload
  add_member_payload=$(cat <<JSON
{"meetingId":"${meeting_id}","userId":"${CURRENT_USER_ID}"}
JSON
)

  curl -sS -X POST "${BASE_URL}/meetings/${meeting_id}/members" \
    -H "Authorization: Bearer ${token}" \
    -H "Content-Type: application/json" \
    -d "${add_member_payload}" >/dev/null || true

  local end_payload
  end_payload=$(cat <<JSON
{"meetingId":"${meeting_id}","transcript":"Action item create task. Decision approve board changes."}
JSON
)

  local end_response
  end_response=$(curl -sS -w "\n%{http_code}" -X POST "${BASE_URL}/meetings/${meeting_id}/end" \
    -H "Authorization: Bearer ${token}" \
    -H "Content-Type: application/json" \
    -d "${end_payload}")

  local end_status
  end_status=$(echo "${end_response}" | tail -n1)
  if [[ "${end_status}" != "200" ]]; then
    echo "End meeting failed: $(echo "${end_response}" | sed '$d')"
    exit 1
  fi

  local summary_payload
  summary_payload=$(cat <<JSON
{"meetingId":"${meeting_id}"}
JSON
)

  local summary_response
  summary_response=$(curl -sS -w "\n%{http_code}" -X POST "${BASE_URL}/summaries" \
    -H "Authorization: Bearer ${token}" \
    -H "Content-Type: application/json" \
    -d "${summary_payload}")

  local summary_status
  summary_status=$(echo "${summary_response}" | tail -n1)
  if [[ "${summary_status}" != "201" ]]; then
    echo "Generate summary failed: $(echo "${summary_response}" | sed '$d')"
    exit 1
  fi

  echo "${meeting_id}"
}

approve_summary_once() {
  local idx="$1"
  local token="$2"
  local meeting_id="$3"

  local payload
  payload=$(cat <<JSON
{"meetingId":"${meeting_id}","response":"APPROVED","comments":"parallel approval ${idx}"}
JSON
)

  local status
  status=$(curl -sS -o /dev/null -w "%{http_code}" -X POST "${BASE_URL}/approvals/summary/${meeting_id}" \
    -H "Authorization: Bearer ${token}" \
    -H "Content-Type: application/json" \
    -d "${payload}")

  echo "${status}"
}

run_parallel_cards() {
  local token="$1"
  local stage_id="$2"
  export -f create_card_once
  export BASE_URL

  seq 1 "${PARALLEL_USERS}" | xargs -P"${PARALLEL_USERS}" -I{} bash -c 'create_card_once "$@"' _ {} "${token}" "${stage_id}"
}

run_parallel_approvals() {
  local token="$1"
  local meeting_id="$2"
  export -f approve_summary_once
  export BASE_URL

  seq 1 "${PARALLEL_USERS}" | xargs -P"${PARALLEL_USERS}" -I{} bash -c 'approve_summary_once "$@"' _ {} "${token}" "${meeting_id}"
}

echo "Seeding user and authenticating against ${BASE_URL}"
register_user
token=$(login_and_capture_identity)

ids=$(create_project_and_stage "${token}")
project_id=$(echo "${ids}" | cut -d'|' -f1)
stage_id=$(echo "${ids}" | cut -d'|' -f3)

echo "Running ${PARALLEL_USERS} concurrent board card creates"
card_results=$(run_parallel_cards "${token}" "${stage_id}")
card_ok=$(echo "${card_results}" | grep -c '^ok$' || true)
card_fail=$(echo "${card_results}" | grep -c '^fail:' || true)

echo "Creating meeting and summary for approval-concurrency test"
meeting_id=$(create_meeting_and_summary "${token}" "${project_id}")

echo "Running ${PARALLEL_USERS} concurrent summary approvals"
approval_results=$(run_parallel_approvals "${token}" "${meeting_id}")
approval_200=$(echo "${approval_results}" | grep -c '^200$' || true)
approval_409=$(echo "${approval_results}" | grep -c '^409$' || true)
approval_other=$(echo "${approval_results}" | grep -vcE '^(200|409)$' || true)

echo "BOARD_CONCURRENCY_OK=${card_ok}"
echo "BOARD_CONCURRENCY_FAIL=${card_fail}"
echo "APPROVAL_HTTP_200=${approval_200}"
echo "APPROVAL_HTTP_409=${approval_409}"
echo "APPROVAL_HTTP_OTHER=${approval_other}"

if [[ "${approval_other}" -ne 0 ]]; then
  echo "APPROVAL_STATUS_CODES_RAW=${approval_results//$'\n'/,}"
fi

if [[ "${card_ok}" -eq "${PARALLEL_USERS}" ]] && [[ $((approval_200 + approval_409)) -eq "${PARALLEL_USERS}" ]] && [[ "${approval_200}" -ge 1 ]] && [[ "${approval_other}" -eq 0 ]]; then
  echo "E2E_CONCURRENCY_RESULT=PASS"
  exit 0
fi

echo "E2E_CONCURRENCY_RESULT=FAIL"
exit 1
