# Delivery Roadmap

## Phase 0 — Architecture foundation

- system design and ADRs;
- multi-module Maven workspace;
- coding standards and CI skeleton;
- local infrastructure topology.

## Phase 1 — Account and ledger core

- account creation and test funding;
- immutable ledger entries;
- available and reserved balances;
- PostgreSQL migrations and integration tests;
- optimistic locking and reconciliation checks.

## Phase 2 — Transfer intake and idempotency

- transfer REST API;
- Redis and PostgreSQL idempotency protection;
- transfer state machine;
- transactional outbox;
- OpenAPI contract.

## Phase 3 — Kafka workflow

- funds reservation events;
- idempotent consumers;
- risk decisions;
- settlement and compensation;
- AsyncAPI contract and dead-letter topics.

## Phase 4 — Resilience and observability

- retry and timeout policies;
- structured JSON logs and correlation propagation;
- Elasticsearch, Logstash, and Kibana dashboards;
- health, metrics, and operational runbooks.

## Phase 5 — Operations console

- React account and transfer views;
- transfer creation and status timeline;
- failure and rejection inspection;
- accessible loading, empty, and error states.

## Phase 6 — Quality proof

- GitHub Actions quality gates;
- end-to-end Docker Compose tests;
- k6 load tests and documented results;
- architecture review and release `v1.0.0`;
- short demonstration video.
