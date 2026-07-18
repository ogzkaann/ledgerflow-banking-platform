# Technology Stack

## Selection principles

The stack is selected to match current Java microservice roles while remaining stable enough for a portfolio project that must be easy to run and explain. Dependencies will be pinned through Maven dependency management and container image tags; floating `latest` tags are not allowed.

## Baseline

| Area | Technology | Baseline | Reason |
| --- | --- | --- | --- |
| Runtime | Java | 21 LTS | Widely adopted modern LTS with records, sealed types, pattern matching, and virtual threads available where appropriate |
| Application framework | Spring Boot | 4.1.0 | Current production release with modern observability and security support |
| Cloud components | Spring Cloud | 2025.1.2 | Compatible release train for Spring Boot 4.1 |
| Build | Apache Maven Wrapper | 3.9.16 | Reproducible builds without requiring a system Maven installation |
| Database | PostgreSQL | 18.4 | Relational source of truth, ACID transactions, constraints, and locking |
| Messaging | Apache Kafka | 4.3.1 | Durable event streaming and asynchronous service communication |
| Cache / coordination | Redis | 8.2 LTS | Idempotency records, rate limiting, and bounded caches; never the financial source of truth |
| Log analytics | Elastic Stack | 9.4.2 | Centralized JSON logs, search, dashboards, and operational investigation |
| Frontend | React | 19.2 | Modern operational console with TypeScript |
| API contracts | OpenAPI | 3.1 | Versioned synchronous HTTP contracts |
| Event contracts | AsyncAPI | 3.x | Versioned Kafka topic and message documentation |
| Database migration | Flyway | Managed by Spring Boot | Repeatable, reviewable schema evolution |
| Resilience | Resilience4j | Managed by Spring Boot ecosystem | Explicit timeouts, retry policies, and circuit breakers for synchronous calls |
| Tests | JUnit 5, AssertJ, Mockito, Testcontainers | Managed versions | Layered automated validation against real infrastructure containers |
| Architecture tests | ArchUnit | Pinned in build | Enforces package and dependency boundaries |
| Load tests | k6 | Pinned container | Repeatable transfer throughput and latency checks |

## Intentional choices

### Kafka instead of RabbitMQ

Kafka is the primary implementation because it demonstrates durable event streams, replay, consumer groups, partitioning, and event-driven workflow design. The architecture keeps message contracts broker-neutral enough that a RabbitMQ adapter can be added later as a comparison exercise.

### PostgreSQL remains the source of truth

Balances, reservations, transfers, outbox records, and processed-message records are persisted in PostgreSQL. Redis may accelerate or protect operations, but losing Redis must not corrupt financial state.

### Java 21 instead of the newest available JDK

The project optimizes for employer relevance and ecosystem compatibility rather than novelty. Java 21 is a mature LTS baseline commonly used in current Spring systems. A Java 25 compatibility build may be added later.

### Monorepo with independently deployable services

A monorepo makes the architecture reviewable and allows one-command local startup. Each service keeps its own domain model, schema migrations, tests, and container image. Shared business-domain libraries are intentionally avoided.
