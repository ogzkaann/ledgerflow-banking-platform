# Testing Strategy

## Test pyramid

### Unit tests

- Account money, mutation, reconciliation, and application orchestration (implemented);
- transfer state-machine transitions;
- deterministic risk rules;
- idempotency decision logic;
- mapping and validation edge cases.

### Slice tests

- Spring MVC request validation and error responses (implemented with the Account PostgreSQL integration fixture);
- JPA mappings and repository queries (implemented for Account Service);
- Kafka serialization and listener configuration;
- Redis adapters.

### Integration tests

Account Service integration tests run real PostgreSQL 18.4 with Testcontainers. They verify Flyway migrations, JPA schema validation, check and unique constraints, atomic commit/rollback, row-lock concurrency, persistence, reconciliation, and ledger pagination/order. H2 is not used. Kafka and Redis containers will be introduced only with their future capabilities.

### Contract tests

The implemented Account endpoints have an OpenAPI 3.1 source-of-truth contract. Automated schema linting and future AsyncAPI compatibility fixtures remain roadmap work and are not current CI claims.

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

ArchUnit currently enforces that the Account domain cannot depend on Spring, JPA, servlet APIs, application services, or adapters.
