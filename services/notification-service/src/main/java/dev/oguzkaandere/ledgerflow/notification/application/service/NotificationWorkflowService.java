package dev.oguzkaandere.ledgerflow.notification.application.service;

import dev.oguzkaandere.ledgerflow.notification.application.event.WorkflowEnvelope;
import dev.oguzkaandere.ledgerflow.notification.domain.model.Notification;
import dev.oguzkaandere.ledgerflow.notification.domain.model.NotificationType;
import dev.oguzkaandere.ledgerflow.notification.domain.port.NotificationRepository;
import io.micrometer.core.instrument.MeterRegistry;
import java.sql.Timestamp;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.function.Supplier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class NotificationWorkflowService {
    public static final String COMPLETED = "ledgerflow.transfer.completed.v1";
    public static final String REJECTED = "ledgerflow.transfer.rejected.v1";
    private static final Logger LOGGER = LoggerFactory.getLogger(NotificationWorkflowService.class);

    private final NotificationRepository notifications;
    private final JdbcTemplate jdbc;
    private final Clock clock;
    private final Supplier<UUID> uuidGenerator;
    private final MeterRegistry metrics;

    public NotificationWorkflowService(
            NotificationRepository notifications,
            JdbcTemplate jdbc,
            Clock clock,
            Supplier<UUID> uuidGenerator,
            MeterRegistry metrics) {
        this.notifications = notifications;
        this.jdbc = jdbc;
        this.clock = clock;
        this.uuidGenerator = uuidGenerator;
        this.metrics = metrics;
    }

    @Transactional
    public void handle(WorkflowEnvelope envelope) {
        if (!COMPLETED.equals(envelope.eventType()) && !REJECTED.equals(envelope.eventType())) {
            throw new IllegalArgumentException("Unsupported Notification Service event type");
        }
        if (Boolean.TRUE.equals(jdbc.queryForObject(
                "SELECT EXISTS (SELECT 1 FROM processed_events WHERE event_id=?)",
                Boolean.class,
                envelope.eventId()))) {
            metrics.counter("kafka.consumer.duplicate", "service", "notification-service")
                    .increment();
            LOGGER.info(
                    "kafka_consumer_duplicate service=notification-service eventId={} eventType={} correlationId={}",
                    envelope.eventId(),
                    envelope.eventType(),
                    envelope.correlationId());
            return;
        }
        var transferNode = envelope.payload().get("transferId");
        if (transferNode == null || !transferNode.isValueNode()) {
            throw new IllegalArgumentException("Notification event transferId is required");
        }
        UUID transferId = UUID.fromString(transferNode.asText());
        boolean completed = COMPLETED.equals(envelope.eventType());
        Instant now = clock.instant();
        notifications.save(new Notification(
                uuidGenerator.get(),
                transferId,
                envelope.eventId(),
                completed ? NotificationType.TRANSFER_COMPLETED : NotificationType.TRANSFER_REJECTED,
                completed ? "COMPLETED" : "REJECTED",
                envelope.correlationId(),
                completed ? "transfer-completed-v1" : "transfer-rejected-v1",
                now));
        jdbc.update(
                "INSERT INTO processed_events(event_id,event_type,processed_at) VALUES (?,?,?)",
                envelope.eventId(),
                envelope.eventType(),
                Timestamp.from(now));
        metrics.counter("kafka.consumer.processed", "service", "notification-service")
                .increment();
        metrics.counter("notifications.recorded", "service", "notification-service")
                .increment();
        LOGGER.info(
                "notification_recorded service=notification-service eventId={} eventType={} transferId={} correlationId={}",
                envelope.eventId(),
                envelope.eventType(),
                transferId,
                envelope.correlationId());
    }

    @Transactional(readOnly = true)
    public List<Notification> findByTransferId(UUID transferId) {
        return notifications.findByTransferId(transferId);
    }
}
