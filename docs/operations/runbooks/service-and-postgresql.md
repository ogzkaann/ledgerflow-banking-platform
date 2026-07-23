# Service and PostgreSQL runbooks

## Service unavailable

Symptoms: `ServiceDown` fires, a Grafana availability panel is zero, readiness
fails, or the gateway returns `502/503`.

1. Identify the failing target in Prometheus: `up == 0`.
2. Check the process and its last structured logs:
   `Get-Content logs/<service>.json -Tail 100`.
3. Call `GET /actuator/health/liveness` and
   `GET /actuator/health/readiness` directly.
4. Inspect owned dependencies with `docker compose ps`.
5. Restart only the failed Java process after recording the error. Do not delete
   a database volume, Kafka topic, outbox row, or processed-event row.
6. Verify readiness, gateway access, outbox drain, and one authorized read.

Do not retry client writes blindly when their HTTP outcome is unknown; use the
same idempotency key and inspect PostgreSQL first. Escalate when repeated restarts
fail, financial invariants are uncertain, or more than one service is unavailable.

## PostgreSQL unavailable

Symptoms: readiness is `DOWN`, Hikari errors appear, or an owning PostgreSQL
container is unhealthy.

1. Identify the owning database; never point a service at another service's DB.
2. Run `docker compose ps` and inspect the specific container:
   `docker compose logs --tail 200 <postgres-service>`.
3. Check host disk space and port conflicts. Do not run destructive SQL.
4. Restart the single container: `docker compose restart <postgres-service>`.
5. Wait for `healthy`, then verify the owning service readiness.
6. Query counts and constraints relevant to the interrupted operation. For money
   movement, reconcile source/destination balances, reservation status, ledger
   references, transfer history, processed event, and outbox row.

Do not manually advance workflow state or delete locks/records. Escalate on
corruption, migration failure, repeated crash recovery, or reconciliation error.
