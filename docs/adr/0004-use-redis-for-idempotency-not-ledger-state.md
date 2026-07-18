# ADR 0004: Use Redis for Idempotency, Not Ledger State

- **Status:** Accepted
- **Date:** 2026-07-18

## Decision

Redis provides fast duplicate-request detection, bounded rate-limiting state, and short-lived query caches. PostgreSQL stores the durable idempotency mapping and all authoritative financial state.

## Consequences

- Redis loss may reduce performance but cannot lose balances or transfers.
- Durable idempotency survives cache eviction.
- Cache invalidation behavior must be tested.
