# Phase 4 verification record

Phase 4 security, observability, and operational-hardening controls were
verified on 2026-07-23 against the complete local platform. This record is
evidence for a portfolio/demo environment, not a production certification.

## Environment

- Eclipse Temurin JDK `25.0.3+9`
- Docker Desktop with isolated Compose project
  `ledgerflow-phase4-verification`
- Keycloak `26.7.0`
- Prometheus `3.13.1`
- Grafana `13.0.2`
- Elasticsearch, Logstash, and Kibana `9.3.6`
- all five application JARs running from the current source tree

Windows reserved the default PostgreSQL and Grafana host ports. Verification
therefore used host-only overrides `15432` through `15435` for PostgreSQL and
`13000` for Grafana. Container ports, service discovery, and checked-in defaults
were unchanged.

## Automated gates

| Gate | Result |
| --- | --- |
| `.\mvnw.cmd -B clean verify` | passed for the complete six-module reactor |
| Unit and integration test reports | 83 tests, 0 failures, 0 errors, 0 skipped |
| Real Kafka/four-database E2E suite | 5 scenarios passed |
| Gateway focused verification | 9 tests passed, including fail-closed rate limiting |
| Redocly Account, Transfer, Notification contracts | valid with zero warnings |
| AsyncAPI event contract | valid; one informational 3.1 upgrade suggestion |
| `docker compose config` with core and observability profile | valid |
| Prometheus configuration and rules via `promtool` | valid; 11 rules loaded |
| Keycloak, Grafana, Kibana, and Elastic JSON assets | valid JSON |
| CycloneDX aggregate SBOM | valid CycloneDX 1.6 JSON; 181 components |
| Gitleaks source-tree scan | no leaks found |
| `git diff --check` | clean |

The final clean reactor run completed in 7 minutes 57 seconds and includes the
Gateway limiter regression tests and all real-infrastructure workflow tests.

## Authentication and authorization

Freshly imported Keycloak data produced RS256 access tokens with:

- issuer `http://localhost:8090/realms/ledgerflow`;
- audience `ledgerflow-api`;
- the expected `ledgerflow-admin`, `ledgerflow-operator`, or
  `ledgerflow-auditor` realm role.

The following requests were exercised through the live Gateway:

| Check | Observed result |
| --- | --- |
| account creation without a token | `401` |
| account creation as auditor | `403` |
| account creation as operator | `201` |
| synthetic funding as operator | `403` |
| direct Account Service request without a token | `401` |
| Account metrics without a token / as admin | `401` / `200` |
| preflight from an unapproved origin | `403` |
| invalid inbound correlation ID | replaced by a generated UUID |

This proves the Gateway boundary and independent downstream resource-server
boundary. Identity-like inbound headers were stripped and were not accepted as
authentication.

## Rate limiting and Redis failure

A burst of 20 authenticated account-create requests produced 14 `429`
responses with `Retry-After: 1`. Redis keys used the
`gateway-rate-limit:*` namespace and authenticated-principal-derived keys.

Redis was then stopped while Transfer Service and the Gateway remained running.
The first exercise revealed that the framework limiter reports backend errors
as allowed responses with a negative remaining count. The custom
`FailClosedRedisRateLimiter` now detects that sentinel, records
`gateway.rate_limit.backend.failures`, and denies the protected write.

The exact outage scenario was repeated after the fix:

- account creation returned `429` with `Retry-After: 1`;
- the account row count remained unchanged;
- Transfer Service readiness remained `200`;
- restoring Redis returned the rate limiter to normal operation.

The dedicated limiter test and application-context test prevent regression to
the framework's fail-open implementation.

## Transfer workflow and durable state

Happy-path evidence:

- source account `e909b106-0969-446d-bd6b-ff0a0b8cb350`;
- destination account `04808627-a48c-4d41-a187-61d9d989591f`;
- transfer `3978f980-05bf-431f-9fe2-827b641746b8`;
- replaying the same idempotency key returned the same transfer;
- history was `PENDING -> FUNDS_RESERVED -> RISK_APPROVED -> SETTLING ->
  COMPLETED`;
- final source balance was `874.50` available and `0.00` reserved;
- final destination balance was `225.50`;
- exactly one source debit, one destination credit, and one completed
  notification existed.

Rejection-path evidence:

- transfer `17c448f9-4ee0-4a08-b133-b843e1646439`;
- history was `PENDING -> FUNDS_RESERVED -> COMPENSATING -> REJECTED`;
- source balance returned to `1000.00` available and `0.00` reserved;
- no transfer debit or credit existed;
- exactly one rejected notification existed.

These results preserve the Phase 3 durable workflow invariants after security
and telemetry were added.

## Observability

- Prometheus reported all five application targets up.
- Workflow counters reported two accepted, one completed, one rejected, one
  approved risk decision, one rejected decision, and two notifications.
- Outbox pending rows returned to zero.
- All 11 alert rules reported healthy.
- A real rate-limit burst moved `RateLimitSpike` from inactive to pending to
  firing after its configured one-minute hold.
- Grafana provisioned the LedgerFlow Overview, Transfer Workflow, and Kafka and
  Outbox dashboards with the Prometheus datasource.
- Logstash ran its configured pipeline with a persisted queue and DLQ.
- Elasticsearch held ECS JSON events from all four workflow services, and
  Kibana provisioned the data view plus correlation and failure saved searches.
- Correlation `phase4-happy` returned 12 events: eight Transfer, two Account,
  one Risk, and one Notification event.
- A structured-log scan found no bearer tokens, client secrets, passwords, or
  raw Kafka payloads.

Logstash 9.3 rejected the legacy `http.host` setting during the first startup.
The configuration now uses `api.http.host`; the monitoring API and pipeline
were verified healthy afterward.

## Scope and limitations

The verification used local service-account clients and local-only demo
secrets. It did not test production identity federation, TLS termination,
Alertmanager delivery, long-term retention, backup restoration, multi-node
Kafka, multi-region behavior, penetration testing, or regulatory controls.
Those remain explicit deployment and governance work rather than claims made by
this repository.
