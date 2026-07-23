# LedgerFlow

LedgerFlow is an educational Java microservice platform that demonstrates a durable,
eventually consistent bank-transfer workflow. It does not process real money and is
not intended for production banking use.

## Implemented services

| Module | Port | Responsibility |
| --- | ---: | --- |
| API Gateway | 8080 | Routes Account, Transfer, and demo Notification APIs |
| Account Service | 8081 | Accounts, balances, immutable ledger, reservations, settlement, compensation |
| Transfer Service | 8082 | Idempotent intake, workflow state/history, commands and terminal events |
| Risk Service | 8083 | Deterministic amount/reference decision with a durable audit record |
| Notification Service | 8084 | Idempotent persistence of final completion/rejection notifications |

Each stateful service owns an independent PostgreSQL database. Transfer Service also
uses Redis as an optional idempotency accelerator; PostgreSQL remains authoritative.
Kafka carries the asynchronous workflow with at-least-once delivery. Transactional
outboxes and same-transaction `processed_events` records provide exactly-once
business effects without claiming exactly-once transport.

Keycloak issues short-lived RS256 tokens for the internal operations API. The
Gateway and every downstream service independently validate issuer, audience,
lifetime, signature algorithm, and realm roles. Redis-backed Gateway policies
rate-limit authenticated callers. Prometheus/Grafana and the Elastic Stack are an
optional Compose profile and never participate in financial transactions.

## Transfer lifecycle

An accepted HTTP request is `PENDING`, not transferred. The happy path is:

```text
PENDING -> FUNDS_RESERVED -> RISK_APPROVED -> SETTLING -> COMPLETED
```

Risk rejection uses compensation:

```text
PENDING -> FUNDS_RESERVED -> COMPENSATING -> REJECTED
```

Reservation failures move directly from `PENDING` to `REJECTED`. Only settlement
creates a source debit and destination credit. Releasing a reservation creates no
transfer ledger entry.

## Prerequisites and verification

- Java 25 LTS
- Git
- Docker Desktop or a compatible Docker Engine

```powershell
.\mvnw.cmd spotless:apply
.\mvnw.cmd clean verify
docker compose config
```

Start the complete local infrastructure:

```powershell
Copy-Item .env.example .env
docker compose up -d
```

Add the optional dashboards and searchable logs with:

```powershell
docker compose --profile observability up -d
```

Run the five applications in separate terminals. Use the `local` profile for
Account Service so its synthetic funding endpoint is available, and add
`observability` when running the optional stack:

```powershell
.\mvnw.cmd -pl services/account-service spring-boot:run "-Dspring-boot.run.profiles=local"
.\mvnw.cmd -pl services/transfer-service spring-boot:run
.\mvnw.cmd -pl services/risk-service spring-boot:run
.\mvnw.cmd -pl services/notification-service spring-boot:run "-Dspring-boot.run.profiles=local"
.\mvnw.cmd -pl services/api-gateway spring-boot:run
```

See [local development](docs/development/local-development.md) for the complete
authenticated demo procedure and environment overrides.

## Contracts and design

- [Kafka AsyncAPI](contracts/asyncapi/ledgerflow-events.yaml)
- [Account OpenAPI](contracts/openapi/account-service.yaml)
- [Transfer OpenAPI](contracts/openapi/transfer-service.yaml)
- [Notification OpenAPI](contracts/openapi/notification-service.yaml)
- [System design](docs/architecture/system-design.md)
- [Event model](docs/architecture/event-model.md)
- [Testing strategy](docs/architecture/testing-strategy.md)
- [Account Service](docs/services/account-service.md)
- [Transfer Service](docs/services/transfer-service.md)
- [Risk Service](docs/services/risk-service.md)
- [Notification Service](docs/services/notification-service.md)
- [Phase 3 verification record](docs/development/phase3-verification.md)
- [Phase 4 verification record](docs/development/phase4-verification.md)
- [Authentication and authorization](docs/security/authentication-authorization.md)
- [Threat model](docs/security/threat-model.md)
- [Observability operations](docs/operations/observability.md)
- [Operational runbooks](docs/operations/runbooks)
- [Technology stack](docs/technology-stack.md)
- [Roadmap](docs/architecture/roadmap.md)

## Repository status

Phases 0 through 4 are implemented. Phase 4 adds defense-in-depth OIDC security,
rate limiting, structured ECS logs, protected Prometheus metrics, provisioned
Grafana/Kibana views, alerts, runbooks, SBOM generation, secret scanning, and
CodeQL. Production deployment, real notification delivery, customer ownership
authorization, regulatory certification, external banking integrations, and
multi-region operation remain deliberately out of scope.
