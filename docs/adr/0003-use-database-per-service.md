# ADR 0003: Use Database per Service

- **Status:** Accepted
- **Date:** 2026-07-18

## Decision

Every service owns its tables and migrations. No service reads or writes another service's schema directly. Local Docker Compose may host the logical databases on one PostgreSQL server, but credentials and schemas remain isolated.

## Consequences

- Service ownership is explicit.
- Cross-service joins are unavailable and read models may be eventually consistent.
- Integration happens through contracts rather than shared persistence.
