# Technology Stack

## Selection principles

Versions were verified on 2026-07-18 using project-owned documentation, release notes, compatibility matrices, and Maven Central metadata. The build uses stable general-availability releases only. Spring Boot dependency management remains authoritative for Java libraries that it tests as a coordinated set; newer standalone library releases are not forced over that tested baseline without an integration need.

## Active build baseline

| Area | Selected version | Build status | Reason |
| --- | --- | --- | --- |
| Java | 25 LTS | Enforced | Latest available LTS, generally available since September 2025 and supported by Spring Boot 4.1 |
| Spring Boot | 4.1.0 | Parent POM | Latest stable production release |
| Spring Cloud | 2025.1.2 | Imported BOM | Latest stable release train and the first 2025.1 service release explicitly compatible with Spring Boot 4.1.0 |
| Maven Wrapper | 3.3.4 | Checked in | Latest stable wrapper tooling |
| Apache Maven | 3.9.16 | Wrapper distribution | Latest recommended production Maven release; Maven 4 remains a preview |
| PostgreSQL | 18.4 | Account and Transfer Compose and Testcontainers | Independent service databases used for local runtime and integration tests |
| Redis Open Source | 8.8.0 | Transfer Compose and Testcontainers | Non-authoritative idempotency acceleration with PostgreSQL fallback |
| Testcontainers | 2.0.5 | Active in Account and Transfer tests | Proves migrations, persistence, Redis behavior, and concurrency |
| Flyway | 12.4.0 | Active in Account and Transfer Services | Schema owner with database-specific PostgreSQL support |

The POM also pins Maven Compiler 3.15.0, Enforcer 3.6.3, Surefire and Failsafe 3.5.6, Spotless 3.8.0, Palantir Java Format 2.96.0, and JaCoCo 0.8.15.

## Planned infrastructure and frontend baselines

These versions are verified targets, not claims that the corresponding infrastructure or application is present:

| Area | Selected version | Status |
| --- | --- | --- |
| Apache Kafka broker | 4.3.1 | Latest supported Kafka bug-fix release; integration deferred |
| React | 19.2 | Latest stable React feature release; frontend deferred |
| Node.js | 24.18.0 LTS | Current production LTS line at verification time; frontend tooling deferred |

Kafka's broker version is deliberately separate from the Kafka client version. When messaging is introduced, the Spring Boot BOM will select the Spring Kafka and Kafka client libraries, while compatibility with the selected broker image will be proven by Testcontainers integration tests.

## Audit corrections

- Java changed from 21 to 25 because 25 is now the current LTS and is within Spring Boot 4.1's supported Java range.
- The previous `3.9.16` "Maven Wrapper" entry conflated two products. Wrapper tooling is 3.3.4; the wrapped Maven distribution is 3.9.16.
- Spring Boot 4.1.0 and Spring Cloud 2025.1.2 were retained after the Spring Cloud 2025.1.2 release notes explicitly confirmed their compatibility.
- Redis changed from 8.2 to 8.8.0. Redis 8.2 was a stable release, but it is no longer the latest stable Open Source line and the Open Source release notes do not designate it as an LTS baseline.
- Flyway and Testcontainers are present in the two stateful implemented services and remain governed by the Spring Boot BOM.
- Kafka and frontend versions remain documentation-only targets until their delivery phases.

## Primary verification sources

- [Oracle Java SE support roadmap](https://www.oracle.com/java/technologies/java-se-support-roadmap.html)
- [OpenJDK 25 project](https://openjdk.org/projects/jdk/25/)
- [Spring Boot project and stable versions](https://spring.io/projects/spring-boot/)
- [Spring Cloud 2025.1.2 release announcement](https://spring.io/blog/2026/06/11/spring-cloud-2025-1-2-aka-oakwood-has-been-released)
- [Apache Maven downloads](https://maven.apache.org/download.cgi)
- [Apache Maven Wrapper downloads](https://maven.apache.org/tools/wrapper/download.cgi)
- [PostgreSQL 18.4 release notes](https://www.postgresql.org/docs/release/18.4/)
- [Apache Kafka downloads](https://kafka.apache.org/community/downloads/)
- [Redis Open Source 8.8 release notes](https://redis.io/docs/latest/operate/oss_and_stack/stack-with-enterprise/release-notes/redisce/redisos-8.8-release-notes/)
- [Testcontainers Java releases](https://github.com/testcontainers/testcontainers-java/releases)
- [Flyway Engine release notes](https://documentation.red-gate.com/flyway/release-notes-and-older-versions/release-notes-for-flyway-engine)
- [React 19.2 release](https://react.dev/blog/2025/10/01/react-19-2)
- [Node.js release status](https://nodejs.org/en/about/previous-releases)

## Architecture choices retained

The project remains a modular monorepo of independently deployable services. Account and Transfer own independent PostgreSQL schemas. Transfer stores pending event intent in an outbox, Redis is never authoritative, no shared business-domain library exists, and both implemented domains remain independent of Spring.
