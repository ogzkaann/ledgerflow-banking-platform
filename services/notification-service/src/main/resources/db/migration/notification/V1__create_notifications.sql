CREATE TABLE notifications (
    notification_id UUID PRIMARY KEY,
    transfer_id UUID NOT NULL,
    event_id UUID NOT NULL UNIQUE,
    type VARCHAR(30) NOT NULL,
    final_transfer_status VARCHAR(20) NOT NULL,
    correlation_id VARCHAR(100) NOT NULL,
    message_template_key VARCHAR(100) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT ck_notification_type CHECK (type IN ('TRANSFER_COMPLETED','TRANSFER_REJECTED')),
    CONSTRAINT ck_notification_status CHECK (final_transfer_status IN ('COMPLETED','REJECTED')),
    CONSTRAINT ck_notification_correlation CHECK (length(btrim(correlation_id)) > 0),
    CONSTRAINT ck_notification_template CHECK (length(btrim(message_template_key)) > 0)
);

CREATE INDEX ix_notifications_transfer_time
    ON notifications (transfer_id, created_at, notification_id);

CREATE TABLE processed_events (
    event_id UUID PRIMARY KEY,
    event_type VARCHAR(100) NOT NULL,
    processed_at TIMESTAMPTZ NOT NULL
);
