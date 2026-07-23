# Observability operations

The observability profile is optional and never participates in a transfer
transaction.

```powershell
docker compose up -d
docker compose --profile observability up -d
```

Host applications must run with `local,observability`. They emit ECS JSON to
`logs/*.json` while retaining console output. Logstash tails those files and
indexes `ledgerflow-logs-YYYY.MM.DD`.

Prometheus runs at `http://localhost:9091`, Grafana at
`http://localhost:3000`, Elasticsearch at `http://localhost:9200`, and Kibana
at `http://localhost:5601`. On Linux, Compose maps
`host.docker.internal` through `host-gateway`. Application ports are template
parameters rendered by the Prometheus container.

Prometheus obtains an admin service-account token through Keycloak and scrapes
the protected `/actuator/prometheus` endpoint for all five applications.
Probes remain public. If Prometheus, Grafana, Elasticsearch, Logstash, or Kibana
fails, the core workflow continues.

## Dashboards and alerts

Provisioned Grafana dashboards:

- LedgerFlow Overview: availability, request rate, 5xx rate, p95 latency, JVM
  heap, and Hikari utilization.
- Transfer Workflow: accepted/completed/rejected, reservations, risk decisions,
  notifications, and completion ratio.
- Kafka and Outbox: pending/failed rows, publish attempts/failures, consumer
  processing/duplicates, listener failures, and DLT activity.

Eleven Prometheus rules cover service availability, outbox backlog/failures,
HTTP error and latency, Kafka/DLT failures, database pools, transfer completion,
authentication failures, and rate limiting. The local environment has no
Alertmanager receiver, so a firing alert is visible but does not notify a human.
Gateway rate-limiter backend failures are exposed as
`gateway.rate.limit.backend.failures`; protected writes are denied while Redis
cannot evaluate the policy.

Thresholds are intentionally sensitive for demonstrations. Production
thresholds require traffic baselines, service-level objectives, multi-window
burn rates, and routed ownership.

## Log search

Kibana provisions the `ledgerflow-logs-*` data view and saved searches for:

- one transfer correlation (`correlationId`);
- errors and DLT activity;
- outbox publication failures.

Useful KQL:

```text
correlationId : "trusted-correlation-id"
transferId : "00000000-0000-0000-0000-000000000000"
message : outbox_publish_failed
log.level : ERROR or message : *dlt*
```

ECS/MDC fields include service/environment, correlation, event, causation, and
transfer identifiers when relevant. Tokens, secrets, request bodies, raw Kafka
payloads, and arbitrary headers are not ingested deliberately.

## Retention and capacity

Prometheus defaults to seven days. Application log rotation retains seven
files/days within a 250 MB cap per process. Elasticsearch demo indices require
operator-managed deletion; a production environment needs an explicit ILM
policy and protected snapshots.

Allocate at least 6 GB to Docker Desktop for the complete local stack. Start
without the `observability` profile when only the money workflow is needed.
