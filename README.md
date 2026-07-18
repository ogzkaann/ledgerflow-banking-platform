# LedgerFlow

LedgerFlow is a portfolio project for exploring resilient Java microservice design in a digital-banking domain. The repository provides a verified multi-module build, five Spring Boot service processes, and a production-style Account Service backed by PostgreSQL.

> **Portfolio scope:** LedgerFlow is an educational system. It does not process real money and is not intended for production banking use.

## Current foundation

The Maven reactor contains these independently packaged applications:

| Module | Application name | Default port | Current capability |
| --- | --- | ---: | --- |
| `api-gateway` | `api-gateway` | 8080 | Reactive Spring Cloud Gateway foundation and health endpoint |
| `account-service` | `account-service` | 8081 | Account creation, reads, immutable ledger, reconciliation, and local/test synthetic funding |
| `transfer-service` | `transfer-service` | 8082 | Spring MVC application foundation and health endpoint |
| `risk-service` | `risk-service` | 8083 | Spring MVC application foundation and health endpoint |
| `notification-service` | `notification-service` | 8084 | Spring MVC application foundation and health endpoint |

The Account Service uses framework-free domain objects, ports and adapters, Flyway-owned PostgreSQL schema, row locking for safe balance mutations, RFC-compatible API errors, and a documented OpenAPI 3.1 contract. Other services remain foundations only. The root build enforces Java and Maven versions, formatting, dependency convergence, unit and integration test lifecycles, and JaCoCo report generation.

## Prerequisites

- Java 25 LTS
- Git
- Docker Desktop or a compatible Docker Engine for Account Service PostgreSQL and the full verification suite
- A network connection for the Maven Wrapper's first run

A system Maven installation and host PostgreSQL installation are not required. Kafka, Redis, Node.js, and frontend tooling are not used yet.

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

## Planned architecture

Later phases will add transfers, reservations, Kafka with transactional outboxes, Redis-backed protective controls, security, centralized observability, and a React operations console. Those capabilities are architecture decisions and roadmap items, not current runtime claims.

Only `account-service` includes database and migration dependencies. No module includes Kafka, Redis, Spring Security, or frontend dependencies yet.

## Documentation

- [Local development](docs/development/local-development.md)
- [Account Service](docs/services/account-service.md)
- [Account Service OpenAPI](contracts/openapi/account-service.yaml)
- [Verified technology stack](docs/technology-stack.md)
- [System design](docs/architecture/system-design.md)
- [Quality attributes](docs/architecture/quality-attributes.md)
- [Event model](docs/architecture/event-model.md)
- [Testing strategy](docs/architecture/testing-strategy.md)
- [Security model](docs/architecture/security.md)
- [Delivery roadmap](docs/architecture/roadmap.md)
- [Architecture Decision Records](docs/adr/)

## Repository status

Phase 0 is complete. The Account and ledger core of Phase 1 is operational and verified; the remaining services and cross-service workflows are intentionally deferred.
