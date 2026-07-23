CREATE TABLE processed_events (
    event_id UUID PRIMARY KEY,
    event_type VARCHAR(100) NOT NULL,
    processed_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT ck_transfer_processed_event_type_not_blank CHECK (length(btrim(event_type)) > 0)
);

CREATE INDEX ix_transfer_processed_events_type_time
    ON processed_events (event_type, processed_at);

CREATE INDEX ix_transfer_outbox_status_order
    ON outbox_events (status, occurred_at, event_id);
