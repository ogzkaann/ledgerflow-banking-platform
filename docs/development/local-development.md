# Local Development

## Current scope

This repository currently builds five minimal Spring Boot applications. It does not start databases, brokers, caches, an Elastic Stack, or a frontend. No Docker installation or `.env` file is required for the current phase.

## Required tools

- Java 25 LTS
- Git
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

`verify` compiles every module, runs unit and integration-test lifecycles, checks dependency convergence and formatting, packages each service, and generates JaCoCo reports. No coverage percentage gate is set while the modules contain only application bootstraps.

## Run a service

Run one module from the repository root:

```bash
./mvnw -pl services/api-gateway spring-boot:run
./mvnw -pl services/account-service spring-boot:run
./mvnw -pl services/transfer-service spring-boot:run
./mvnw -pl services/risk-service spring-boot:run
./mvnw -pl services/notification-service spring-boot:run
```

In PowerShell, replace `./mvnw` with `.\mvnw.cmd`.

| Service | Default URL |
| --- | --- |
| API Gateway | `http://localhost:8080/actuator/health` |
| Account Service | `http://localhost:8081/actuator/health` |
| Transfer Service | `http://localhost:8082/actuator/health` |
| Risk Service | `http://localhost:8083/actuator/health` |
| Notification Service | `http://localhost:8084/actuator/health` |

Override a port with the `SERVER_PORT` environment variable. Example for PowerShell:

```powershell
$env:SERVER_PORT = "9081"
.\mvnw.cmd -pl services/account-service spring-boot:run
```

## Configuration and planned infrastructure

The committed `.env.example` reserves names for later local-infrastructure work. Current services do not load it and do not consume those variables. Copying it to `.env` has no effect yet.

PostgreSQL drivers, Flyway, Kafka clients, Redis clients, infrastructure containers, and Testcontainers fixtures will be added only alongside the adapters and integration tests that need them.

## Common failures

- If Enforcer reports the wrong Java version, point `JAVA_HOME` to a Java 25 JDK and ensure its `bin` directory is first on `PATH`.
- If the wrapper cannot download Maven, verify access to `https://repo.maven.apache.org` through the local proxy or firewall.
- If a service port is occupied, set `SERVER_PORT` to an unused port before starting that service.
- If formatting verification fails, run the documented `spotless:apply` command and review the resulting diff.
