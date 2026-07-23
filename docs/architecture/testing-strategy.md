# Testing Strategy

## Test pyramid

### Unit tests

- Account money, mutation, reconciliation, and application orchestration (implemented);
- Transfer money invariants, complete state-transition matrix, and canonical request fingerprints (implemented);
- deterministic risk rules;
- idempotency decision logic;
- mapping and validation edge cases.

### Slice tests

- Spring MVC request validation and error responses (implemented with the Account PostgreSQL integration fixture);
- JPA mappings and repository queries (implemented for Account Service);
- Kafka serialization and listener configuration;
- Transfer API validation and Redis degradation adapters (implemented).

### Integration tests

Account Service integration tests run real PostgreSQL 18.4 with Testcontainers. Transfer integration tests run independent PostgreSQL 18.4 and Redis 8.8.0 containers and verify Flyway/JPA validation, JSONB outbox intent, REST behavior, durable replay/conflict semantics, cache repair and TTL, and concurrent same-key creation. H2 is not used. Kafka containers remain deferred.

### Contract tests

The implemented Account and Transfer endpoints have OpenAPI 3.1 source-of-truth contracts. Automated schema linting and future AsyncAPI compatibility fixtures remain roadmap work.

### End-to-end tests

A Docker Compose environment will validate:

1. successful transfer;
2. insufficient funds rejection;
3. risk rejection and reservation release;
4. duplicate HTTP request;
5. duplicate Kafka event;
6. service restart during processing;
7. poison message routed to a dead-letter topic.

### Performance tests

k6 scripts report throughput, latency percentiles, and error rates. Performance results will be committed as reproducible reports, not screenshots without configuration.

## Coverage policy

Coverage is a diagnostic, not the goal. Domain and application layers require branch coverage for critical workflows. Infrastructure code requires meaningful integration coverage. A high percentage cannot replace missing failure tests.

ArchUnit enforces framework and adapter isolation for both Account and Transfer domains, including a prohibition on Transfer importing Account packages.
