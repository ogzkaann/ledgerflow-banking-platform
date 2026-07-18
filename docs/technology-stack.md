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
| Testcontainers | 2.0.5 | Spring Boot managed; use deferred | Latest stable Java release and already aligned by the Spring Boot 4.1 BOM |
| Flyway | 12.4.0 | Spring Boot managed; use deferred | Framework-tested version; upstream 12.11.0 is newer, but overriding the BOM before persistence integration would add unverified risk |

The POM also pins Maven Compiler 3.15.0, Enforcer 3.6.3, Surefire and Failsafe 3.5.6, Spotless 3.8.0, Palantir Java Format 2.96.0, and JaCoCo 0.8.15.

## Planned infrastructure and frontend baselines

These versions are verified targets, not claims that the corresponding infrastructure or application is present:

| Area | Selected version | Status |
| --- | --- | --- |
| PostgreSQL | 18.4 | Latest stable PostgreSQL 18 maintenance release; integration deferred |
| Apache Kafka broker | 4.3.1 | Latest supported Kafka bug-fix release; integration deferred |
| Redis Open Source | 8.8.0 | Latest stable general-availability line; integration deferred |
| React | 19.2 | Latest stable React feature release; frontend deferred |
| Node.js | 24.18.0 LTS | Current production LTS line at verification time; frontend tooling deferred |

Kafka's broker version is deliberately separate from the Kafka client version. When messaging is introduced, the Spring Boot BOM will select the Spring Kafka and Kafka client libraries, while compatibility with the selected broker image will be proven by Testcontainers integration tests.

## Audit corrections

- Java changed from 21 to 25 because 25 is now the current LTS and is within Spring Boot 4.1's supported Java range.
- The previous `3.9.16` "Maven Wrapper" entry conflated two products. Wrapper tooling is 3.3.4; the wrapped Maven distribution is 3.9.16.
- Spring Boot 4.1.0 and Spring Cloud 2025.1.2 were retained after the Spring Cloud 2025.1.2 release notes explicitly confirmed their compatibility.
- Redis changed from 8.2 to 8.8.0. Redis 8.2 was a stable release, but it is no longer the latest stable Open Source line and the Open Source release notes do not designate it as an LTS baseline.
- Flyway and Testcontainers were removed from service POMs until persistence integration tests exist. Their selected library versions remain governed by the Spring Boot BOM.
- PostgreSQL, Kafka, Redis, and frontend versions remain documentation-only targets until their delivery phases add executable configuration and tests.

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

The project remains a modular monorepo of independently deployable services. Each future stateful service owns its database. Kafka will use transactional outboxes, Redis will never be an authoritative financial store, no shared business-domain library will be introduced, and domain logic will remain independent of Spring when implementation begins.
