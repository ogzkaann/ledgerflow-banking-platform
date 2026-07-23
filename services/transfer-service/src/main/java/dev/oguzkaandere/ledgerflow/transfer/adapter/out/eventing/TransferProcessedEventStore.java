package dev.oguzkaandere.ledgerflow.transfer.adapter.out.eventing;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class TransferProcessedEventStore {
    private final JdbcTemplate jdbc;

    public TransferProcessedEventStore(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
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
}
