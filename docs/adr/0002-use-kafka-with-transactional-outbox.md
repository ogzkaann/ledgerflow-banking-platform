# ADR 0002: Use Kafka with Transactional Outbox

- **Status:** Accepted
- **Date:** 2026-07-18

## Context

A database update followed by a direct Kafka publish can fail between the two operations, leaving committed business state without an event or an event without corresponding state.

## Decision

Each event-producing service writes business state and an outbox record in one PostgreSQL transaction. A separate publisher sends pending outbox records to Kafka and marks them published. Consumers remain idempotent because publishing may be repeated.

## Consequences

- Database and event intent remain atomic.
- Delivery is at-least-once and requires deduplication.
- Outbox lag becomes an operational metric.
- The initial implementation uses a polling publisher; Debezium CDC may be evaluated later.
