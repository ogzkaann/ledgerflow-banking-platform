# Risk Service

Risk Service owns one durable, explainable decision per transfer. It consumes only
`ledgerflow.account.funds-reserved.v1`, persists the decision and rule version,
records the input event, and creates the decision event in one PostgreSQL
transaction.

## Deterministic rules

Rules are evaluated in this order:

1. reject an amount greater than `${RISK_MAXIMUM_AMOUNT:5000.00}` with
   `ABOVE_MAXIMUM_AMOUNT`;
2. reject a reference containing `${RISK_BLOCKED_MARKER:RISK-REJECT}` with
   `BLOCKED_REFERENCE`;
3. otherwise approve with `RULES_PASSED`.

The default version is `${RISK_RULE_VERSION:risk-rules-v1}`. There is no randomness,
AI service, or public decision-mutation endpoint.

The result is either `ledgerflow.risk.approved.v1` or
`ledgerflow.risk.rejected.v1`, published through the Risk outbox to
`ledgerflow.risk.events.v1` with transfer ID as the record key.

## Persistence and delivery

`V1__create_risk_workflow.sql` creates `risk_decisions`, `processed_events`, and
`outbox_events` with unique transfer/event constraints and bounded polling
indexes. Flyway owns the schema; JPA validates it.

If the same input event is delivered again, no work repeats. If a different event
ID describes a transfer that already has a decision, it records the input as
processed and retains the original logical decision without a second outbox row.
Database and outbox failure roll back the complete local transaction.

Malformed or unsupported input follows the account-events retry/DLT policy. Known
account events not relevant to risk are ignored; unknown event types are rejected.

## Local use and verification

```powershell
docker compose up -d risk-postgres kafka kafka-init
.\mvnw.cmd -pl services/risk-service spring-boot:run
```

Readiness includes Risk PostgreSQL and Kafka. Unit tests prove rule ordering and
threshold behavior. PostgreSQL Testcontainers tests prove decision/outbox
atomicity, blocked-marker rejection, uniqueness, and durable deduplication. The
cross-service E2E test proves the decision drives settlement or compensation.

Risk exposes no business API. It independently validates Keycloak JWTs on
operational endpoints: metrics/info require admin and liveness/readiness are
public. Prometheus records approved/rejected decisions, Kafka listener/DLT, outbox
publication, HTTP/JVM, and Hikari signals. The `observability` profile writes
bounded ECS JSON workflow context.
