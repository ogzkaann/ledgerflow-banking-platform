# LedgerFlow

LedgerFlow is a portfolio project for exploring resilient Java microservice design in a digital-banking domain. The repository provides a verified multi-module build and production-style Account and Transfer Services.

> **Portfolio scope:** LedgerFlow is an educational system. It does not process real money and is not intended for production banking use.

## Current foundation

The Maven reactor contains these independently packaged applications:

| Module | Application name | Default port | Current capability |
| --- | --- | ---: | --- |
| `api-gateway` | `api-gateway` | 8080 | Reactive Spring Cloud Gateway foundation and health endpoint |
| `account-service` | `account-service` | 8081 | Account creation, reads, immutable ledger, reconciliation, and local/test synthetic funding |
| `transfer-service` | `transfer-service` | 8082 | Idempotent transfer intake, state history, PostgreSQL, Redis, and transactional outbox |
| `risk-service` | `risk-service` | 8083 | Spring MVC application foundation and health endpoint |
| `notification-service` | `notification-service` | 8084 | Spring MVC application foundation and health endpoint |

Account and Transfer Services use independent framework-free domain models, ports and adapters, Flyway-owned PostgreSQL schemas, RFC-compatible errors, and OpenAPI 3.1 contracts. Transfer acceptance creates a `PENDING` workflow request and pending outbox event; it does not move money.

## Prerequisites

- Java 25 LTS
- Git
- Docker Desktop or a compatible Docker Engine for PostgreSQL, Redis, and the full verification suite
- A network connection for the Maven Wrapper's first run

A system Maven installation and host PostgreSQL or Redis installations are not required. Kafka, Node.js, and frontend tooling are not used yet.

## Build and test

On macOS, Linux, or Git Bash:

```bash
./mvnw clean verify
./mvnw test
```

On Windows PowerShell:

```powershell
.\mvnw.cmd clean verify
.\mvnw.cmd test
```

Start PostgreSQL and run the Account Service local profile from the repository root:

```bash
docker compose up -d postgres
./mvnw -pl services/account-service spring-boot:run -Dspring-boot.run.profiles=local
```

Then query `http://localhost:8081/actuator/health/readiness`. See the [Account Service guide](docs/services/account-service.md) for API examples and the [local development guide](docs/development/local-development.md) for Windows equivalents.

Start Transfer PostgreSQL and Redis with `docker compose up -d transfer-postgres redis`, then run `./mvnw -pl services/transfer-service spring-boot:run -Dspring-boot.run.profiles=local`.

## Planned architecture

Later phases will add reservations, Kafka publication and consumers, risk, settlement, security, centralized observability, and a React operations console.

Account and Transfer Services own separate databases. Redis accelerates Transfer idempotency without becoming authoritative. No module includes Kafka, Spring Security, or frontend dependencies yet.

## Documentation

- [Local development](docs/development/local-development.md)
- [Account Service](docs/services/account-service.md)
- [Account Service OpenAPI](contracts/openapi/account-service.yaml)
- [Transfer Service](docs/services/transfer-service.md)
- [Transfer Service OpenAPI](contracts/openapi/transfer-service.yaml)
- [Verified technology stack](docs/technology-stack.md)
- [System design](docs/architecture/system-design.md)
- [Quality attributes](docs/architecture/quality-attributes.md)
- [Event model](docs/architecture/event-model.md)
- [Testing strategy](docs/architecture/testing-strategy.md)
- [Security model](docs/architecture/security.md)
- [Delivery roadmap](docs/architecture/roadmap.md)
- [Architecture Decision Records](docs/adr/)

## Repository status

Phases 0, 1, and 2 are complete. Transfer requests remain `PENDING`; cross-service Kafka workflow execution is deferred to Phase 3.
