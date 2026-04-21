#!/usr/bin/env bash
set -euo pipefail

EXISTING_VARS=$(aws lambda get-function-configuration --region us-east-2 --function-name flowboard-backend --query 'Environment.Variables' --output json)
UPDATED_VARS=$(python3 - <<'PY' "$EXISTING_VARS"
import json
import sys

variables = json.loads(sys.argv[1])
variables['BEDROCK_MODEL_ID'] = 'amazon.nova-micro-v1:0'
print(json.dumps(variables, separators=(',', ':')))
PY
)

aws lambda update-function-configuration \
  --region us-east-2 \
  --function-name flowboard-backend \
  --environment "{\"Variables\":$UPDATED_VARS}"

aws lambda wait function-updated --region us-east-2 --function-name flowboard-backend
aws lambda get-function-configuration --region us-east-2 --function-name flowboard-backend --query '{FunctionName:FunctionName,State:State,LastUpdateStatus:LastUpdateStatus,Environment:Environment.Variables}' --output json
