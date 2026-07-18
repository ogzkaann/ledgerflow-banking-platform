# Local Development

## Current scope

This repository builds five Spring Boot applications. `account-service` implements account and immutable-ledger behavior with PostgreSQL; the other four applications remain foundations. Kafka, Redis, the Elastic Stack, and the frontend are not present.

## Required tools

- Java 25 LTS
- Git
- Docker Desktop or a compatible Docker Engine for Account Service development and full verification
- A network connection on the first build so the wrapper can download Apache Maven 3.9.16 and dependencies

Check the environment on macOS, Linux, or Git Bash:

```bash
./scripts/check-environment.sh
```

Check it on Windows PowerShell:

```powershell
.\scripts\check-environment.ps1
```

## Build commands

Use the checked-in Maven Wrapper. A system Maven installation is neither required nor used.

| Task | macOS, Linux, or Git Bash | Windows PowerShell |
| --- | --- | --- |
| Full clean verification | `./mvnw clean verify` | `.\mvnw.cmd clean verify` |
| Unit tests | `./mvnw test` | `.\mvnw.cmd test` |
| Apply Java formatting | `./mvnw spotless:apply` | `.\mvnw.cmd spotless:apply` |
| Show dependency tree | `./mvnw dependency:tree` | `.\mvnw.cmd dependency:tree` |

`verify` compiles every module, runs unit and integration-test lifecycles, checks dependency convergence and formatting, packages each service, and generates JaCoCo reports. Account integration tests start PostgreSQL 18.4 with Testcontainers and fail visibly when Docker is unavailable. No coverage percentage gate is set; behavioral failure tests remain the primary quality signal.

Validate the Docker Compose model independently with `docker compose config`.

## Run the Account Service

Start the pinned PostgreSQL container from the repository root:

```bash
cp .env.example .env
docker compose up -d postgres
docker compose ps
```

PowerShell equivalent:

```powershell
Copy-Item .env.example .env
docker compose up -d postgres
docker compose ps
```

The `.env` file is optional because Compose and the application have matching safe local defaults. Start the service with its `local` profile so the explicitly synthetic funding endpoint is registered:

```bash
./mvnw -pl services/account-service spring-boot:run -Dspring-boot.run.profiles=local
```

```powershell
.\mvnw.cmd -pl services/account-service spring-boot:run "-Dspring-boot.run.profiles=local"
```

Useful URLs:

| Resource | URL |
| --- | --- |
| Liveness | `http://localhost:8081/actuator/health/liveness` |
| Readiness, including PostgreSQL | `http://localhost:8081/actuator/health/readiness` |
| Accounts | `http://localhost:8081/api/v1/accounts` |

See the [Account Service guide](../services/account-service.md) and [OpenAPI contract](../../contracts/openapi/account-service.yaml) for requests and response behavior.

Stop the application with `Ctrl+C`. Stop PostgreSQL while retaining its named volume with `docker compose down`. Adding `-v` deletes the local database volume.

## Run a service

Run one module from the repository root:

```bash
./mvnw -pl services/api-gateway spring-boot:run
./mvnw -pl services/transfer-service spring-boot:run
./mvnw -pl services/risk-service spring-boot:run
./mvnw -pl services/notification-service spring-boot:run
```

In PowerShell, replace `./mvnw` with `.\mvnw.cmd`.

| Service | Default URL |
| --- | --- |
| API Gateway | `http://localhost:8080/actuator/health` |
| Account Service | Use the `local` profile workflow above |
| Transfer Service | `http://localhost:8082/actuator/health` |
| Risk Service | `http://localhost:8083/actuator/health` |
| Notification Service | `http://localhost:8084/actuator/health` |

Override a port with the `SERVER_PORT` environment variable. Example for PowerShell:

```powershell
$env:SERVER_PORT = "9081"
.\mvnw.cmd -pl services/account-service spring-boot:run
```

## Account database configuration

`account-service` accepts `ACCOUNT_DB_URL`, `ACCOUNT_DB_USERNAME`, and `ACCOUNT_DB_PASSWORD`. Compose accepts `ACCOUNT_POSTGRES_DB`, `ACCOUNT_POSTGRES_USER`, `ACCOUNT_POSTGRES_PASSWORD`, and `ACCOUNT_POSTGRES_PORT` from `.env`.

If the Compose port is changed from 5432, also export a matching `ACCOUNT_DB_URL` before running Maven; Compose `.env` interpolation does not export variables into the Maven process. Kafka and Redis clients or containers will be added only with the future features that require them.

## Common failures

- If Enforcer reports the wrong Java version, point `JAVA_HOME` to a Java 25 JDK and ensure its `bin` directory is first on `PATH`.
- If the wrapper cannot download Maven, verify access to `https://repo.maven.apache.org` through the local proxy or firewall.
- If a service port is occupied, set `SERVER_PORT` to an unused port before starting that service.
- If Account Service readiness is down, run `docker compose ps` and inspect PostgreSQL with `docker compose logs postgres`.
- If Testcontainers cannot connect, start Docker and confirm `docker info` succeeds before rerunning `verify`.
- If formatting verification fails, run the documented `spotless:apply` command and review the resulting diff.
