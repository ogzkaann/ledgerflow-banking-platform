#!/usr/bin/env bash
set -euo pipefail

until curl --silent --fail --user "elastic:${ELASTIC_PASSWORD}" \
  http://kibana:5601/api/status >/dev/null; do
  sleep 5
done

curl --silent --fail --user "elastic:${ELASTIC_PASSWORD}" \
  --request POST \
  --header "kbn-xsrf: ledgerflow-setup" \
  --form file=@/setup/ledgerflow.ndjson \
  "http://kibana:5601/api/saved_objects/_import?overwrite=true" >/dev/null
