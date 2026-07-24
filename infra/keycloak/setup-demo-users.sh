#!/usr/bin/env bash
set -euo pipefail

KCADM="/opt/keycloak/bin/kcadm.sh"
SERVER_URL="${KEYCLOAK_SERVER_URL:-http://localhost:8090}"
REALM="${LEDGERFLOW_REALM:-ledgerflow}"
USERS_URL="${SERVER_URL}/admin/realms/${REALM}/users"
CLIENTS_URL="${SERVER_URL}/admin/realms/${REALM}/clients"
AUTH=(
  --no-config
  --server "${SERVER_URL}"
  --realm master
  --client "${KEYCLOAK_BOOTSTRAP_CLIENT_ID}"
  --secret "${KEYCLOAK_BOOTSTRAP_CLIENT_SECRET}"
)

ensure_spa_role_scope() {
  local client_id
  local role
  local role_scope_file
  local separator=""

  client_id="$("${KCADM}" get "${CLIENTS_URL}" \
    "${AUTH[@]}" \
    --query "clientId=ledgerflow-spa" \
    --fields id \
    --format csv \
    --noquotes | tail -n 1)"

  if [[ ! "${client_id}" =~ ^[0-9a-fA-F-]{36}$ ]]; then
    echo "Unable to resolve the ledgerflow-spa client." >&2
    return 1
  fi

  role_scope_file="$(mktemp)"
  trap 'rm -f "${role_scope_file}"' RETURN

  printf '[' >"${role_scope_file}"
  for role in ledgerflow-operator ledgerflow-auditor ledgerflow-admin; do
    printf '%s' "${separator}" >>"${role_scope_file}"
    "${KCADM}" get "${SERVER_URL}/admin/realms/${REALM}/roles/${role}" \
      "${AUTH[@]}" >>"${role_scope_file}"
    separator=","
  done
  printf ']' >>"${role_scope_file}"

  "${KCADM}" create \
    "${CLIENTS_URL}/${client_id}/scope-mappings/realm" \
    "${AUTH[@]}" \
    --file "${role_scope_file}"
}

ensure_user() {
  local username="$1"
  local password="$2"
  local role="$3"
  local user_id
  local display_name

  user_id="$("${KCADM}" get "${USERS_URL}" \
    "${AUTH[@]}" \
    --query "username=${username}" \
    --query "exact=true" \
    --fields id \
    --format csv \
    --noquotes | tail -n 1)"

  if [[ ! "${user_id}" =~ ^[0-9a-fA-F-]{36}$ ]]; then
    user_id="$("${KCADM}" create "${USERS_URL}" \
      "${AUTH[@]}" \
      --set "username=${username}" \
      --set "enabled=true" \
      --set "emailVerified=true" \
      --id)"
  fi

  display_name="${username^}"
  "${KCADM}" update "${USERS_URL}/${user_id}" \
    "${AUTH[@]}" \
    --set "enabled=true" \
    --set "firstName=${display_name}" \
    --set "lastName=Demo" \
    --set "email=${username}@ledgerflow.local" \
    --set "emailVerified=true"

  "${KCADM}" set-password \
    "${AUTH[@]}" \
    --target-realm "${REALM}" \
    --userid "${user_id}" \
    --new-password "${password}" \
    --temporary=false

  "${KCADM}" add-roles \
    "${AUTH[@]}" \
    --target-realm "${REALM}" \
    --uid "${user_id}" \
    --rolename "${role}"
}

ensure_spa_role_scope
ensure_user operator "${LEDGERFLOW_OPERATOR_PASSWORD}" ledgerflow-operator
ensure_user auditor "${LEDGERFLOW_AUDITOR_PASSWORD}" ledgerflow-auditor
ensure_user admin "${LEDGERFLOW_ADMIN_PASSWORD}" ledgerflow-admin

echo "LedgerFlow local demo users are ready."
