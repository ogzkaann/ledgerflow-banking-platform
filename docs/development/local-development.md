# Local Development

## Requirements

Use Java 25 LTS, the checked-in Maven Wrapper, Git, and Docker Desktop (or a
compatible Docker Engine). No system Maven, PostgreSQL, Redis, or Kafka installation
is required.

```powershell
.\scripts\check-environment.ps1
.\mvnw.cmd clean verify
docker compose config
```

## Local infrastructure

Copy the safe demo defaults, then start four independent PostgreSQL databases,
Redis, a single-node Kafka 4.1.2 KRaft broker, and deterministic topic creation:

```powershell
Copy-Item .env.example .env
docker compose up -d
docker compose ps
docker compose logs kafka-init
```

Default host ports are Account PostgreSQL 5432, Transfer PostgreSQL 5433, Risk
PostgreSQL 5434, Notification PostgreSQL 5435, Redis 6379, and Kafka 9092.
`.env.example` documents every override. Automatic topic creation is disabled;
`kafka-init` creates four workflow and four DLT topics with three partitions.
This is a local/demo topology, not a production Kafka cluster.

## Run applications

Run each command in its own terminal:

```powershell
.\mvnw.cmd -pl services/account-service spring-boot:run "-Dspring-boot.run.profiles=local"
.\mvnw.cmd -pl services/transfer-service spring-boot:run
.\mvnw.cmd -pl services/risk-service spring-boot:run
.\mvnw.cmd -pl services/notification-service spring-boot:run "-Dspring-boot.run.profiles=local"
.\mvnw.cmd -pl services/api-gateway spring-boot:run
```

The Gateway is `http://localhost:8080`; direct service ports are 8081–8084.
Readiness is `/actuator/health/readiness` on each service. Gateway routes only:

```text
/api/v1/accounts/**
/api/v1/transfers/**
/api/v1/notifications/**
```

Risk and Actuator are intentionally not exposed through broad Gateway routes.
Environment-configurable upstream URLs are `ACCOUNT_SERVICE_URL`,
`TRANSFER_SERVICE_URL`, and `NOTIFICATION_SERVICE_URL`.

## Manual happy-path demo

Create two accounts through the Gateway:

```powershell
$source = Invoke-RestMethod -Method Post -Uri http://localhost:8080/api/v1/accounts `
  -ContentType application/json `
  -Body '{"ownerReference":"demo-source","currency":"EUR"}'
$destination = Invoke-RestMethod -Method Post -Uri http://localhost:8080/api/v1/accounts `
  -ContentType application/json `
  -Body '{"ownerReference":"demo-destination","currency":"EUR"}'

Invoke-RestMethod -Method Post `
  -Uri "http://localhost:8080/api/v1/accounts/$($source.accountId)/test-funding" `
  -ContentType application/json `
  -Body '{"amount":"1000.00","reference":"demo-source-funding"}'
Invoke-RestMethod -Method Post `
  -Uri "http://localhost:8080/api/v1/accounts/$($destination.accountId)/test-funding" `
  -ContentType application/json `
  -Body '{"amount":"100.00","reference":"demo-destination-funding"}'
```

Accept a transfer. The first response is `PENDING`:

```powershell
$headers = @{
  "Idempotency-Key" = "demo-transfer-001"
  "X-Correlation-Id" = "demo-correlation-001"
}
$body = @{
  sourceAccountId = $source.accountId
  destinationAccountId = $destination.accountId
  amount = "125.50"
  currency = "EUR"
  reference = "demo-approved"
} | ConvertTo-Json
$transfer = Invoke-RestMethod -Method Post -Uri http://localhost:8080/api/v1/transfers `
  -Headers $headers -ContentType application/json -Body $body
```

Poll `GET /api/v1/transfers/{id}` until `COMPLETED`, then inspect:

```powershell
Invoke-RestMethod "http://localhost:8080/api/v1/transfers/$($transfer.transferId)"
Invoke-RestMethod "http://localhost:8080/api/v1/transfers/$($transfer.transferId)/history"
Invoke-RestMethod "http://localhost:8080/api/v1/accounts/$($source.accountId)"
Invoke-RestMethod "http://localhost:8080/api/v1/accounts/$($destination.accountId)"
Invoke-RestMethod "http://localhost:8080/api/v1/accounts/$($source.accountId)/ledger?page=0&size=20"
Invoke-RestMethod "http://localhost:8080/api/v1/accounts/$($destination.accountId)/ledger?page=0&size=20"
Invoke-RestMethod "http://localhost:8080/api/v1/notifications?transferId=$($transfer.transferId)"
```

Expected balances are source `874.50` available and `0.00` reserved, destination
`225.50` available, with one source debit and destination credit.

For risk compensation, create another funded pair and use reference `RISK-REJECT`
(or amount above 5000.00 while funding enough to reserve it). The final status is
`REJECTED`, the source available balance returns to its starting value, reserved is
zero, no transfer debit/credit exists, and one rejection notification is stored.

## Database and Kafka inspection

Risk has no public API. Inspect the durable decision and outboxes directly in their
own local databases:

```powershell
docker compose exec risk-postgres psql -U ledgerflow_risk -d ledgerflow_risk `
  -c "select transfer_id,outcome,reason,rule_version,correlation_id from risk_decisions;"
docker compose exec postgres psql -U ledgerflow -d ledgerflow_account `
  -c "select event_type,status,publish_attempt_count from outbox_events order by occurred_at;"
docker compose exec transfer-postgres psql -U ledgerflow_transfer -d ledgerflow_transfer `
  -c "select event_type,status,publish_attempt_count from outbox_events order by occurred_at;"
docker compose exec risk-postgres psql -U ledgerflow_risk -d ledgerflow_risk `
  -c "select event_type,status,publish_attempt_count from outbox_events order by occurred_at;"
```

The same correlation ID should appear in Transfer, Account, Risk, and Notification
records/logs.

## Resilience checks

Stopping Kafka does not roll back an already committed local transaction:

```powershell
docker compose stop kafka
# create an event-producing request while Kafka is down
# inspect its PENDING or FAILED outbox row
docker compose start kafka
docker compose run --rm kafka-init
```

After restart, the publisher retries the same event ID and the workflow resumes.
Replaying an already-consumed record must not change balances, history, decisions,
ledger entries, or notifications. DLT records can be inspected with:

```powershell
docker compose exec kafka /opt/kafka/bin/kafka-console-consumer.sh `
  --bootstrap-server kafka:29092 `
  --topic ledgerflow.transfer.commands.dlt.v1 --from-beginning
```

These are bounded recovery guarantees, not a distributed exactly-once claim.

## Cleanup and common failures

`docker compose down` retains named database/Kafka volumes. `docker compose down -v`
deletes local data and should be used only when that loss is intended.

- Wrong Java: point `JAVA_HOME` to a Java 25 JDK.
- Port collision: change the corresponding `.env` port and matching service URL.
- Unready database/Kafka: inspect `docker compose ps` and service logs.
- Redis down: Transfer remains safe and ready through PostgreSQL, with slower
  idempotency lookup.
- Formatting failure: run `.\mvnw.cmd spotless:apply` and review the diff.
