#!/usr/bin/env bash
set -euo pipefail

KCADM="/opt/keycloak/bin/kcadm.sh"
CONFIG="/tmp/ledgerflow-kcadm.config"
SERVER_URL="${KEYCLOAK_SERVER_URL:-http://keycloak:8080}"
REALM="${LEDGERFLOW_REALM:-ledgerflow}"

"${KCADM}" config credentials \
  --config "${CONFIG}" \
  --server "${SERVER_URL}" \
  --realm master \
  --user "${KEYCLOAK_ADMIN_USERNAME}" \
  --password "${KEYCLOAK_ADMIN_PASSWORD}"

ensure_user() {
  local username="$1"
  local password="$2"
  local role="$3"
  local user_id

  user_id="$("${KCADM}" get users \
    --config "${CONFIG}" \
    --realm "${REALM}" \
    --query "username=${username}" \
    --query "exact=true" \
    --fields id \
    --format csv \
    --noquotes | tail -n 1)"

  if [[ ! "${user_id}" =~ ^[0-9a-fA-F-]{36}$ ]]; then
    user_id="$("${KCADM}" create users \
      --config "${CONFIG}" \
      --realm "${REALM}" \
      --set "username=${username}" \
      --set "enabled=true" \
      --set "emailVerified=true" \
      --id)"
  fi

  "${KCADM}" set-password \
    --config "${CONFIG}" \
    --realm "${REALM}" \
    --userid "${user_id}" \
    --new-password "${password}" \
    --temporary=false

  "${KCADM}" add-roles \
    --config "${CONFIG}" \
    --realm "${REALM}" \
    --uid "${user_id}" \
    --rolename "${role}"
}

ensure_user operator "${LEDGERFLOW_OPERATOR_PASSWORD}" ledgerflow-operator
ensure_user auditor "${LEDGERFLOW_AUDITOR_PASSWORD}" ledgerflow-auditor
ensure_user admin "${LEDGERFLOW_ADMIN_PASSWORD}" ledgerflow-admin

echo "LedgerFlow local demo users are ready."
