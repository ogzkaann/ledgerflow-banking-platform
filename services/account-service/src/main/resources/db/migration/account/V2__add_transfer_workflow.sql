CREATE TABLE transfer_reservations (
    reservation_id UUID PRIMARY KEY,
    transfer_id UUID NOT NULL UNIQUE,
    source_account_id UUID NOT NULL REFERENCES accounts(id) ON DELETE RESTRICT,
    destination_account_id UUID NOT NULL REFERENCES accounts(id) ON DELETE RESTRICT,
    amount NUMERIC(19,2) NOT NULL,
    currency VARCHAR(3) NOT NULL,
    status VARCHAR(20) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT ck_reservation_accounts_different CHECK (source_account_id <> destination_account_id),
    CONSTRAINT ck_reservation_amount_positive CHECK (amount > 0),
    CONSTRAINT ck_reservation_currency CHECK (currency IN ('EUR','USD','GBP')),
    CONSTRAINT ck_reservation_status CHECK (status IN ('RESERVED','SETTLED','RELEASED')),
    CONSTRAINT ck_reservation_version_non_negative CHECK (version >= 0)
);

CREATE INDEX ix_reservations_source_status ON transfer_reservations (source_account_id, status);
CREATE INDEX ix_reservations_destination_status ON transfer_reservations (destination_account_id, status);

CREATE TABLE processed_events (
    event_id UUID PRIMARY KEY,
    event_type VARCHAR(100) NOT NULL,
    processed_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT ck_account_processed_event_type_not_blank CHECK (length(btrim(event_type)) > 0)
);

CREATE TABLE outbox_events (
    event_id UUID PRIMARY KEY,
    aggregate_type VARCHAR(50) NOT NULL,
    aggregate_id UUID NOT NULL,
    event_type VARCHAR(100) NOT NULL,
    event_version INTEGER NOT NULL,
    payload JSONB NOT NULL,
    status VARCHAR(20) NOT NULL,
    occurred_at TIMESTAMPTZ NOT NULL,
    published_at TIMESTAMPTZ,
    publish_attempt_count INTEGER NOT NULL DEFAULT 0,
    CONSTRAINT ck_account_outbox_event_version CHECK (event_version > 0),
    CONSTRAINT ck_account_outbox_status CHECK (status IN ('PENDING','PUBLISHED','FAILED')),
    CONSTRAINT ck_account_outbox_attempts CHECK (publish_attempt_count >= 0),
    CONSTRAINT ck_account_outbox_published CHECK (
        (status = 'PUBLISHED' AND published_at IS NOT NULL)
        OR (status <> 'PUBLISHED' AND published_at IS NULL)
    )
);

CREATE INDEX ix_account_outbox_pending
    ON outbox_events (status, occurred_at, event_id);
