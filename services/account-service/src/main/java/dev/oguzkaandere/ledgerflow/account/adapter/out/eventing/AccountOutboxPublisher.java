package dev.oguzkaandere.ledgerflow.account.adapter.out.eventing;

import java.sql.Timestamp;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class AccountOutboxPublisher {
    private static final Logger LOGGER = LoggerFactory.getLogger(AccountOutboxPublisher.class);
    private static final String TOPIC = "ledgerflow.account.events.v1";

    private final JdbcTemplate jdbc;
    private final KafkaTemplate<String, String> kafka;
    private final int batchSize;
    private final Duration acknowledgementTimeout;

    public AccountOutboxPublisher(
            JdbcTemplate jdbc,
            KafkaTemplate<String, String> kafka,
            @Value("${ledgerflow.outbox.batch-size:50}") int batchSize,
            @Value("${ledgerflow.outbox.ack-timeout:10s}") Duration acknowledgementTimeout) {
        this.jdbc = jdbc;
        this.kafka = kafka;
        this.batchSize = batchSize;
        this.acknowledgementTimeout = acknowledgementTimeout;
    }

    @Scheduled(fixedDelayString = "${ledgerflow.outbox.poll-interval:500ms}")
    @Transactional
    public void publishPending() {
        List<OutboxRow> rows = jdbc.query(
                """
                SELECT event_id, aggregate_id, event_type, payload::text
                FROM outbox_events
                WHERE status IN ('PENDING','FAILED')
                ORDER BY occurred_at, event_id
                FOR UPDATE SKIP LOCKED
                LIMIT ?
                """,
                (result, row) -> new OutboxRow(
                        result.getObject("event_id", UUID.class),
                        result.getObject("aggregate_id", UUID.class),
                        result.getString("event_type"),
                        result.getString("payload")),
                batchSize);
        for (OutboxRow row : rows) {
            try {
                kafka.send(TOPIC, row.aggregateId().toString(), row.payload())
                        .get(acknowledgementTimeout.toMillis(), TimeUnit.MILLISECONDS);
                jdbc.update("""
                        UPDATE outbox_events
                        SET status='PUBLISHED', published_at=?, publish_attempt_count=publish_attempt_count+1
                        WHERE event_id=?
                        """, Timestamp.from(Instant.now()), row.eventId());
                LOGGER.info(
                        "outbox_published service=account-service eventId={} eventType={} transferId={} status=PUBLISHED",
                        row.eventId(),
                        row.eventType(),
                        row.aggregateId());
            } catch (Exception exception) {
                jdbc.update("""
                        UPDATE outbox_events
                        SET status='FAILED', published_at=NULL, publish_attempt_count=publish_attempt_count+1
                        WHERE event_id=?
                        """, row.eventId());
                LOGGER.warn(
                        "outbox_publish_failed service=account-service eventId={} eventType={} transferId={} exceptionType={}",
                        row.eventId(),
                        row.eventType(),
                        row.aggregateId(),
                        exception.getClass().getSimpleName());
            }
        }
    }

    private record OutboxRow(UUID eventId, UUID aggregateId, String eventType, String payload) {}
}
