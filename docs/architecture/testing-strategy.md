# Testing Strategy

LedgerFlow tests business invariants at the cheapest boundary that proves them,
then repeats critical money paths against real infrastructure. H2 and mocked Kafka
are not substitutes for the main integration coverage.

## Layers

- Plain unit tests cover money arithmetic, account and reservation guards,
  transfer transition legality, risk-rule determinism, fingerprints, and
  reconciliation.
- ArchUnit keeps Account, Transfer, and Risk domains independent of Spring,
  persistence adapters, and each other.
- PostgreSQL 18.4 Testcontainers run for Account, Transfer, Risk, and Notification.
  Flyway migration and JPA validation execute in every service fixture.
- Redis 8.8.0 Testcontainers prove cache hit/miss, repair, TTL, and graceful
  PostgreSQL fallback.
- Apache Kafka 4.1.2 Testcontainers prove JSON publication, transfer-ID keys,
  broker acknowledgement before outbox publication, DLT routing, and the complete
  asynchronous workflow.
- Gateway tests inspect the configured route definitions and ensure only Account,
  Transfer, and Notification business paths are exposed.

## Financial correctness

Account integration tests prove reservation business outcomes, pessimistic locks,
unique reservations, guarded settlement/release, duplicate commands, deterministic
ledger references, rollback, and reconciliation after settlement. Releasing funds
must not append transfer ledger entries.

Transfer integration tests prove every workflow transition, two-step approval,
immutable sequence history, stale/duplicate handling, atomic processed-event and
outbox writes, and terminal-state protection.

Risk tests prove threshold approval/rejection, blocked markers, stable rule
version, one decision per transfer, duplicate delivery, and rollback if outbox
creation fails. Notification tests prove one record per final event and no
intermediate notification.

## Full Kafka workflow

`KafkaWorkflowE2EIT` starts four independent PostgreSQL containers, Redis, and a
real Kafka broker, then starts all four business service contexts:

1. happy path starts source at `1000.00`, destination at `100.00`, transfers
   `125.50`, and asserts `874.50`/`0.00` source, `225.50` destination,
   `COMPLETED`, one debit, one credit, one notification, and published outboxes;
2. blocked-marker rejection proves reservation then compensation, restored
   `1000.00`/`0.00`, no transfer ledger rows, `REJECTED`, and one notification;
3. malformed input is recovered to the matching DLT with original-record metadata.

All asynchronous assertions use bounded Awaitility polling. Tests never use long,
arbitrary sleeps.

## Failure model

The suite distinguishes expected business rejection from technical failure:

- insufficient funds, missing/inactive accounts, invalid pairs, currency mismatch,
  and risk rejection produce normal workflow events;
- duplicate IDs and stale valid events acknowledge without repeated mutation;
- Kafka send failure leaves an outbox row retryable with the same event ID;
- a service restart reads committed pending rows from PostgreSQL;
- malformed or unsupported messages reach DLT after the configured classification;
- local transaction failure rolls back processed event, business data, and outbox.

The project claims at-least-once transport and idempotent business effects—not a
distributed exactly-once transaction.

## Verification command

```powershell
.\mvnw.cmd spotless:apply
.\mvnw.cmd clean verify
docker compose config
git diff --check
```

CI runs the Maven verification lifecycle from a clean checkout. Coverage is a
diagnostic; passing percentages cannot replace missing invariant/failure tests.
