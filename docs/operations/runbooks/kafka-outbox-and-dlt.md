# Kafka, outbox, and DLT runbooks

## Kafka unavailable

Symptoms: Kafka readiness degrades, outbox failures rise, consumer processing
stops, and accepted transfers remain in an intermediate state.

1. Inspect `docker compose ps kafka kafka-init` and
   `docker compose logs --tail 200 kafka`.
2. Confirm durable acceptance in Transfer PostgreSQL before retrying the client.
3. Check `outbox_pending`, `outbox_failed`, and listener-failure metrics.
4. Restart Kafka only: `docker compose restart kafka`.
5. Do not recreate topics. Wait for broker health and confirm all eight topics:
   `docker compose exec kafka /opt/kafka/bin/kafka-topics.sh --bootstrap-server localhost:29092 --list`.
6. Verify backlog returns to zero and the transfer reaches a terminal state.

Escalate if partitions are unavailable, records are lost, or financial
reconciliation fails.

## Outbox backlog

Symptoms: `OutboxBacklogGrowing` fires or `outbox_pending` remains above zero.

1. Correlate `outbox.publish.attempts`, failures, and latency in Grafana.
2. Search Kibana for `message : outbox_publish_failed`.
3. Verify Kafka health and service readiness.
4. Inspect oldest rows read-only, ordered by `occurred_at,event_id`.
5. Restore the dependency and let the existing bounded publisher retry.
6. Verify every row becomes `PUBLISHED` once acknowledged and the same event ID
   was retained.

Do not manually mark rows published, delete rows, or run multiple unbounded
publishers. Escalate when backlog age exceeds the business recovery objective.

## Failed outbox rows

Symptoms: `OutboxFailedEvents` fires or `outbox_failed > 0`.

1. Search the event ID in structured logs and inspect `exceptionType`.
2. Confirm the payload is a valid stored envelope without printing it into a
   public terminal or ticket.
3. Correct broker/network/configuration failure.
4. Allow normal polling to select `FAILED` rows with `SKIP LOCKED`.
5. Verify attempt count increments, status becomes `PUBLISHED`, and downstream
   processed-event deduplication prevents duplicate effects.

Do not edit payloads or reset attempt counts casually. Escalate malformed durable
payloads as a software/data incident.

## DLT message

Symptoms: `DeadLetterMessagesDetected` fires or DLT publication appears in logs.

1. Record original topic, partition, offset, event ID/correlation ID when
   available, exception type, and DLT topic.
2. Search the correlation across all services and determine whether the message
   was technical failure or malicious/malformed input.
3. Verify expected business rejections did not enter DLT.
4. Quarantine the record. Correct the producer/consumer/schema defect and test
   the fix using synthetic data.
5. Replay only with an approved, bounded procedure preserving the original
   event ID, then verify idempotency and financial state.

Never bulk replay or delete a DLT/topic during diagnosis. Escalate every unknown
production-like DLT and any DLT involving ambiguous money state.
