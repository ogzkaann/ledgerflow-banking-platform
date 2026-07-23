package dev.oguzkaandere.ledgerflow.risk.adapter.out.eventing;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import tools.jackson.databind.ObjectMapper;

@Repository
public class RiskEventStore {
    private final JdbcTemplate jdbc;
    private final ObjectMapper mapper;

    public RiskEventStore(JdbcTemplate jdbc, ObjectMapper mapper) {
        this.jdbc = jdbc;
        this.mapper = mapper;
    }

    public boolean processed(UUID eventId) {
        return Boolean.TRUE.equals(jdbc.queryForObject(
                "SELECT EXISTS (SELECT 1 FROM processed_events WHERE event_id=?)", Boolean.class, eventId));
    }

    public void markProcessed(UUID eventId, String eventType, Instant now) {
        jdbc.update(
                "INSERT INTO processed_events(event_id,event_type,processed_at) VALUES (?,?,?)",
                eventId,
                eventType,
                Timestamp.from(now));
    }

    public UUID append(
            String eventType,
            UUID transferId,
            String correlationId,
            String causationId,
            Map<String, Object> payload,
            Instant now) {
        UUID eventId = UUID.randomUUID();
        Map<String, Object> envelope = Map.of(
                "eventId", eventId,
                "eventType", eventType,
                "eventVersion", 1,
                "occurredAt", now,
                "correlationId", correlationId,
                "causationId", causationId,
                "producer", "risk-service",
                "payload", payload);
        jdbc.update("""
                INSERT INTO outbox_events(
                    event_id,aggregate_type,aggregate_id,event_type,event_version,payload,status,
                    occurred_at,publish_attempt_count
                ) VALUES (?, 'TRANSFER', ?, ?, 1, ?::jsonb, 'PENDING', ?, 0)
                """, eventId, transferId, eventType, mapper.writeValueAsString(envelope), Timestamp.from(now));
        return eventId;
    }

    public int pendingCount() {
        return jdbc.queryForObject("SELECT count(*) FROM outbox_events WHERE status='PENDING'", Integer.class);
    }

    public int failedCount() {
        return jdbc.queryForObject("SELECT count(*) FROM outbox_events WHERE status='FAILED'", Integer.class);
    }
}
