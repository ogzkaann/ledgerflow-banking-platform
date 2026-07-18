# Testing Strategy

## Test pyramid

### Unit tests

- domain invariants and money calculations;
- transfer state-machine transitions;
- deterministic risk rules;
- idempotency decision logic;
- mapping and validation edge cases.

### Slice tests

- Spring MVC request validation and error responses;
- JPA mappings and repository queries;
- Kafka serialization and listener configuration;
- Redis adapters.

### Integration tests

Testcontainers will run real PostgreSQL, Kafka, and Redis instances. Integration tests verify migrations, locking behavior, outbox publication, duplicate message handling, and retry/dead-letter behavior.

### Contract tests

OpenAPI and AsyncAPI schemas are validated in CI. Producer and consumer fixtures ensure backward-compatible message evolution.

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
