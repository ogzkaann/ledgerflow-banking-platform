# LedgerFlow

LedgerFlow is a portfolio project for exploring resilient Java microservice design in a digital-banking domain. The repository currently provides a verified build and five minimal Spring Boot service foundations; it does not implement banking workflows yet.

> **Portfolio scope:** LedgerFlow is an educational system. It does not process real money and is not intended for production banking use.

## Current foundation

The Maven reactor contains these independently packaged applications:

| Module | Application name | Default port | Current capability |
| --- | --- | ---: | --- |
| `api-gateway` | `api-gateway` | 8080 | Reactive Spring Cloud Gateway foundation and health endpoint |
| `account-service` | `account-service` | 8081 | Spring MVC application foundation and health endpoint |
| `transfer-service` | `transfer-service` | 8082 | Spring MVC application foundation and health endpoint |
| `risk-service` | `risk-service` | 8083 | Spring MVC application foundation and health endpoint |
| `notification-service` | `notification-service` | 8084 | Spring MVC application foundation and health endpoint |

Each module has a context-load test. The root build enforces Java and Maven versions, formatting, dependency convergence, unit and integration test lifecycles, and JaCoCo report generation.

## Prerequisites

- Java 25 LTS
- Git
- A network connection for the Maven Wrapper's first run

A system Maven installation is not required. Docker, PostgreSQL, Kafka, Redis, and Node.js are not required for the current foundation.

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

Run a service from the repository root, for example:

```bash
./mvnw -pl services/account-service spring-boot:run
```

Then query `http://localhost:8081/actuator/health`. See the [local development guide](docs/development/local-development.md) for all module commands and Windows equivalents.

## Planned architecture

Later phases will add account and transfer domain behavior, database-per-service persistence, Kafka with transactional outboxes, Redis-backed protective controls, security, observability, containerized local infrastructure, and a React operations console. Those capabilities are architecture decisions and roadmap items, not current runtime claims.

No database drivers, migration engine, message broker clients, or Redis clients are included in service modules yet. They will be introduced with executable configuration, migrations, and integration tests in the relevant delivery phase.

## Documentation

- [Local development](docs/development/local-development.md)
- [Verified technology stack](docs/technology-stack.md)
- [System design](docs/architecture/system-design.md)
- [Quality attributes](docs/architecture/quality-attributes.md)
- [Event model](docs/architecture/event-model.md)
- [Testing strategy](docs/architecture/testing-strategy.md)
- [Security model](docs/architecture/security.md)
- [Delivery roadmap](docs/architecture/roadmap.md)
- [Architecture Decision Records](docs/adr/)

## Repository status

Phase 0 establishes the build, application skeletons, developer workflow, and continuous verification. Business features and local infrastructure are intentionally deferred.
