# Delivery Roadmap

## Phase 0 — Build and architecture foundation

Status: complete.

- multi-module Java 25 / Spring Boot 4.1 build;
- architecture documents, ADRs, Maven quality gates, and CI.

## Phase 1 — Account and ledger core

Status: complete.

- account creation and local/test funding;
- immutable ledger and materialized balances;
- reconciliation, Flyway PostgreSQL, OpenAPI, and integration tests.

## Phase 2 — Transfer intake and idempotency

Status: complete.

- transfer REST API and guarded state model;
- PostgreSQL durable idempotency with optional Redis acceleration;
- initial transactional outbox and OpenAPI contract.

## Phase 3 — Kafka workflow

Status: complete after the full E2E suite passes.

- Account reservation, settlement, compensation, and immutable transfer ledger;
- idempotent Account, Transfer, Risk, and Notification consumers;
- deterministic durable risk decisions;
- transactional outbox publishers for Transfer, Account, and Risk;
- completed/rejected notifications;
- Kafka KRaft Compose topology, deterministic topics, retries, DLTs, and readiness;
- AsyncAPI contract and real Kafka/four-database end-to-end verification.

## Phase 4 — Security and observability

Status: complete.

- authentication and authorization;
- structured JSON logging and expanded Micrometer dashboards;
- Elasticsearch, Logstash, and Kibana;
- production-oriented alerting and operational runbooks.

## Phase 5 — Operations console

- React account and transfer views;
- transfer creation and status timeline;
- failure/rejection inspection and accessible UI states.

## Phase 6 — Quality proof

- k6 load tests and reproducible reports;
- architecture review and production-readiness gap analysis;
- release `v1.0.0` and demonstration video.

Real payment networks, real notification delivery, regulatory certification,
multi-region deployment, and production Kafka remain explicitly outside the
portfolio roadmap until separately designed.
