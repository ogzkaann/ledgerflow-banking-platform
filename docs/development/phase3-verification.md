# Phase 3 verification record

This record captures the local Phase 3 verification performed on 2026-07-23.
The environment was isolated under the Compose project
`ledgerflow-phase3-verify`; no existing default-port stack was reused.

## Environment

- Java: Eclipse Temurin 25.0.3
- Spring Boot: 4.1.0
- PostgreSQL: 18.4, one container and database per stateful service
- Redis: 8.8.0
- Kafka: Apache Kafka 4.1.2 in KRaft mode
- Infrastructure ports: PostgreSQL `55432`-`55435`, Redis `56379`, Kafka
  `59092`
- Application ports: Gateway `58080`, Account `58081`, Transfer `58082`,
  Risk `58083`, Notification `58084`

The infrastructure was started with:

```powershell
$env:ACCOUNT_POSTGRES_PORT = "55432"
$env:TRANSFER_POSTGRES_PORT = "55433"
$env:RISK_POSTGRES_PORT = "55434"
$env:NOTIFICATION_POSTGRES_PORT = "55435"
$env:TRANSFER_REDIS_PORT = "56379"
$env:KAFKA_PORT = "59092"
docker compose -p ledgerflow-phase3-verify up -d
```

`docker compose ps -a` reported all long-running containers healthy and
`kafka-init` exited with code 0. `kafka-topics.sh --list` returned all four
workflow topics and all four DLTs. Each application returned `UP` from its
Actuator health endpoint before business verification began.

All business requests below went through the API Gateway. Account creation and
funding used `POST /api/v1/accounts` and the local-profile
`POST /api/v1/accounts/{id}/test-funding`; transfers used
`POST /api/v1/transfers` with explicit `Idempotency-Key` and
`X-Correlation-Id` headers.

## Happy path

Observed identifiers:

| Item | Value |
| --- | --- |
| Source account | `3872a992-6e7b-469e-8b98-614db12a73c3` |
| Destination account | `3cbba65d-edf6-4c0e-9c68-74ffe9521826` |
| Transfer | `d95c9c13-a376-49ef-a8c5-c647b7789ce3` |
| Correlation ID | `manual-happy2-correlation-20260723` |

The source began at `1000.00 EUR`, the destination at `100.00 EUR`, and the
transfer amount was `125.50 EUR`. The intake response was `PENDING`.

Observed history:

```text
PENDING -> FUNDS_RESERVED -> RISK_APPROVED -> SETTLING -> COMPLETED
```

Observed final state:

| Assertion | Observed value |
| --- | --- |
| Source available / reserved | `874.50` / `0.00` |
| Destination available / reserved | `225.50` / `0.00` |
| Source transfer debit rows | `1` |
| Destination transfer credit rows | `1` |
| Reservation | `SETTLED` |
| Risk decision | `APPROVED`, `RULES_PASSED`, `risk-rules-v1` |
| Completion notifications | `1` |

Transfer, Account, Risk, and Notification persistence and logs all retained
`manual-happy2-correlation-20260723`. Transfer wrote three outbox events,
Account wrote two, and Risk wrote one; every related row was `PUBLISHED` with
`publish_attempt_count = 1`.

## Risk rejection and compensation

Observed identifiers:

| Item | Value |
| --- | --- |
| Source account | `8ed1cfc8-23d4-4e6a-af4a-af034f35a80b` |
| Destination account | `3976dd7f-dfa9-452b-b263-acb704c83ab0` |
| Transfer | `10489e13-f709-4014-860d-1f50f3e1cae4` |
| Correlation ID | `manual-reject-correlation-20260723` |

The source began at `1000.00 EUR`. The `125.50 EUR` request used reference
`RISK-REJECT`, and the intake response was `PENDING`.

Observed history:

```text
PENDING -> FUNDS_RESERVED -> COMPENSATING -> REJECTED
```

Observed final state:

| Assertion | Observed value |
| --- | --- |
| Source available / reserved | `1000.00` / `0.00` |
| Destination available | `0.00` |
| Transfer debit/credit rows | `0` |
| Reservation | `RELEASED` |
| Risk decision | `REJECTED`, `BLOCKED_REFERENCE`, `risk-rules-v1` |
| Rejection notifications | `1` |

All related outbox rows were `PUBLISHED`; the retryable outbox backlog count
was zero in Transfer, Account, and Risk.

## Kafka outage and backlog recovery

Accounts were created and funded before stopping the broker. Kafka was then
stopped with:

```powershell
docker stop ledgerflow-phase3-verify-kafka-1
```

Transfer `71e61ac3-c7b7-4368-9cc9-20b665d70932` was accepted through the
Gateway in 55 ms while Kafka was unavailable. PostgreSQL immediately showed:

```text
transfer: PENDING
outbox:   PENDING | publish_attempt_count=0 | ledgerflow.transfer.initiated.v1
```

This demonstrated that the local intake transaction did not depend on broker
availability. After:

```powershell
docker start ledgerflow-phase3-verify-kafka-1
```

Kafka returned to `healthy`, the existing outbox event was published on attempt
2, and the workflow reached `COMPLETED`. Final source state was
`150.00 available / 0.00 reserved`, destination available was `50.00`, and one
completion notification existed. The subsequent settlement-requested and
completed outbox events each published on attempt 1.

## Keyed duplicate replay

The original `ledgerflow.transfer.initiated.v1` envelope for happy-path
transfer `d95c9c13-a376-49ef-a8c5-c647b7789ce3` was read from the durable
Transfer outbox and replayed to `ledgerflow.transfer.commands.v1` with the
transfer ID as its Kafka key:

```powershell
Write-Output "$transferId`t$payload" |
  docker exec -i ledgerflow-phase3-verify-kafka-1 `
    /opt/kafka/bin/kafka-console-producer.sh `
    --bootstrap-server localhost:29092 `
    --topic ledgerflow.transfer.commands.v1 `
    --property parse.key=true `
    --property "key.separator=`t"
```

The Account metric `kafka.consumer.duplicate` increased from `2` to `3`.
Before/after database snapshots were identical:

```text
source available/reserved: 874.50 / 0.00
destination available:     225.50
transfer ledger rows:      2
reservation:               SETTLED
transfer status/version:   COMPLETED / 4
transfer history rows:     5
risk decisions:            1
notifications:             1
```

No balance movement, ledger entry, state-history row, risk decision, or
notification was duplicated.

## Automated and static validation

The verification sequence included:

```powershell
.\mvnw.cmd spotless:apply
.\mvnw.cmd clean verify
docker compose config --quiet
.\mvnw.cmd -pl services/account-service,services/transfer-service,services/risk-service,services/notification-service dependency:tree
git diff --check
```

The clean reactor passed. The real-Kafka workflow suite covered approved
settlement, risk compensation, insufficient-funds business rejection, malformed
message DLT routing with original-record metadata, and keyed duplicate
redelivery. A separate real-Kafka integration test started a fresh Transfer
Service application context against a durable pre-existing outbox row and
verified publication after restart.
