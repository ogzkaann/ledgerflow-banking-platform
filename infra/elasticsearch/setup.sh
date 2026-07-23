#!/usr/bin/env bash
set -euo pipefail

until curl --silent --fail --user "elastic:${ELASTIC_PASSWORD}" \
  http://elasticsearch:9200/_cluster/health >/dev/null; do
  sleep 5
done

curl --silent --fail --user "elastic:${ELASTIC_PASSWORD}" \
  --request POST \
  --header "Content-Type: application/json" \
  --data "{\"password\":\"${KIBANA_SYSTEM_PASSWORD}\"}" \
  http://elasticsearch:9200/_security/user/kibana_system/_password >/dev/null

curl --silent --fail --user "elastic:${ELASTIC_PASSWORD}" \
  --request PUT \
  --header "Content-Type: application/json" \
  --data '{"cluster":["monitor","manage_index_templates"],"indices":[{"names":["ledgerflow-logs-*"],"privileges":["auto_configure","create_index","write","view_index_metadata"]}]}' \
  http://elasticsearch:9200/_security/role/ledgerflow_logstash >/dev/null

curl --silent --fail --user "elastic:${ELASTIC_PASSWORD}" \
  --request PUT \
  --header "Content-Type: application/json" \
  --data "{\"password\":\"${LOGSTASH_PASSWORD}\",\"roles\":[\"ledgerflow_logstash\"]}" \
  http://elasticsearch:9200/_security/user/ledgerflow_logstash >/dev/null

curl --silent --fail --user "elastic:${ELASTIC_PASSWORD}" \
  --request PUT \
  --header "Content-Type: application/json" \
  --data-binary @/setup/ledgerflow-index-template.json \
  http://elasticsearch:9200/_index_template/ledgerflow-logs >/dev/null
