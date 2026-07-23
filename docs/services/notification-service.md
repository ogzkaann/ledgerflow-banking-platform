# Notification Service

Notification Service is an idempotent terminal-event consumer. It records demo
notifications; it does not send email, SMS, push messages, or contact an external
vendor.

It consumes only:

- `ledgerflow.transfer.completed.v1`;
- `ledgerflow.transfer.rejected.v1`.

For each event it stores notification ID, transfer ID, event ID, notification
type, final transfer status, correlation ID, message-template key, and creation
time. The notification and `processed_events` row commit in one PostgreSQL
transaction. Event ID uniqueness means redelivery creates one record. Intermediate
events and known unrelated topics are not consumed.

## Demo inspection API

The read-only local/test-oriented endpoint is:

```http
GET /api/v1/notifications?transferId={transferId}
```

It is routed by the API Gateway for demonstration and inspection. There is no
notification creation or mutation endpoint.

## Persistence, failures, and verification

`V1__create_notifications.sql` creates `notifications` and `processed_events`, with
unique event constraints and a transfer/time index. Flyway owns the schema and JPA
uses `ddl-auto: validate`.

Malformed, unknown, or unsupported terminal events use bounded retry and
`ledgerflow.transfer.events.dlt.v1`. PostgreSQL and Kafka contribute to readiness.

```powershell
docker compose up -d notification-postgres kafka kafka-init
.\mvnw.cmd -pl services/notification-service spring-boot:run "-Dspring-boot.run.profiles=local"
```

PostgreSQL integration tests cover completion, rejection, duplicate delivery,
ignored intermediate events, persistence, and the inspection API. The real Kafka
E2E suite proves one notification after completed and compensated workflows.
