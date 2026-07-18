# Quality Attributes

## Correctness

Financial state must not rely on floating-point arithmetic. Monetary values use `BigDecimal` with explicit currency and scale rules. Database constraints, state-machine guards, and ledger reconciliation checks protect invariants.

## Reliability

The platform must tolerate duplicated Kafka messages, repeated HTTP requests, consumer restarts, delayed events, and temporary dependency failures without double settlement.

## Observability

Every request and event carries:

- `correlationId` for the end-to-end business flow;
- `eventId` for message-level deduplication;
- `causationId` and `traceparent` where available;
- service name, environment, and version in structured logs.

Operational dashboards will expose transfer throughput, completion latency, rejection reasons, consumer lag, dead-letter volume, and error rates.

## Security

The platform uses least-privilege service credentials, secret injection through environment variables, input validation, non-sensitive logs, and authenticated public APIs. Security controls must not be simulated by comments alone; they require tests or runnable configuration.

## Maintainability

Services follow ports-and-adapters boundaries:

- domain code does not depend on Spring;
- application use cases coordinate domain behavior;
- adapters implement HTTP, Kafka, persistence, and Redis integration;
- architectural rules are enforced with ArchUnit.

## Performance targets

Initial local targets, validated by k6:

- transfer submission API p95 below 250 ms under 100 requests/second;
- no lost accepted transfers during controlled service restarts;
- sustained Kafka processing without growing consumer lag after load stops;
- account query p95 below 150 ms with warm cache.

These are engineering targets, not claims of production banking capacity.
