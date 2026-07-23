CREATE TABLE transfers (
    id UUID PRIMARY KEY,
    source_account_id UUID NOT NULL,
    destination_account_id UUID NOT NULL,
    amount NUMERIC(19, 2) NOT NULL,
    currency VARCHAR(3) NOT NULL,
    reference VARCHAR(100) NOT NULL,
    status VARCHAR(24) NOT NULL,
    correlation_id VARCHAR(100) NOT NULL,
    version BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT ck_transfers_accounts_different CHECK (source_account_id <> destination_account_id),
    CONSTRAINT ck_transfers_amount_positive CHECK (amount > 0),
    CONSTRAINT ck_transfers_currency CHECK (currency IN ('EUR', 'USD', 'GBP')),
    CONSTRAINT ck_transfers_reference_not_blank CHECK (length(btrim(reference)) > 0),
    CONSTRAINT ck_transfers_status CHECK (
        status IN ('PENDING', 'FUNDS_RESERVED', 'RISK_APPROVED', 'SETTLING', 'COMPLETED', 'REJECTED', 'COMPENSATING', 'EXPIRED')
    ),
    CONSTRAINT ck_transfers_correlation_not_blank CHECK (length(btrim(correlation_id)) > 0),
    CONSTRAINT ck_transfers_version_non_negative CHECK (version >= 0)
);

CREATE INDEX ix_transfers_status_created_at ON transfers (status, created_at, id);

CREATE TABLE transfer_state_history (
    id UUID PRIMARY KEY,
    transfer_id UUID NOT NULL,
    from_status VARCHAR(24),
    to_status VARCHAR(24) NOT NULL,
    reason VARCHAR(100) NOT NULL,
    occurred_at TIMESTAMPTZ NOT NULL,
    sequence BIGINT NOT NULL,
    CONSTRAINT fk_transfer_history_transfer FOREIGN KEY (transfer_id) REFERENCES transfers (id) ON DELETE RESTRICT,
    CONSTRAINT uk_transfer_history_sequence UNIQUE (transfer_id, sequence),
    CONSTRAINT ck_transfer_history_from_status CHECK (
        from_status IS NULL OR from_status IN ('PENDING', 'FUNDS_RESERVED', 'RISK_APPROVED', 'SETTLING', 'COMPLETED', 'REJECTED', 'COMPENSATING', 'EXPIRED')
    ),
    CONSTRAINT ck_transfer_history_to_status CHECK (
        to_status IN ('PENDING', 'FUNDS_RESERVED', 'RISK_APPROVED', 'SETTLING', 'COMPLETED', 'REJECTED', 'COMPENSATING', 'EXPIRED')
    ),
    CONSTRAINT ck_transfer_history_reason_not_blank CHECK (length(btrim(reason)) > 0),
    CONSTRAINT ck_transfer_history_sequence_non_negative CHECK (sequence >= 0)
);

CREATE INDEX ix_transfer_history_order ON transfer_state_history (transfer_id, sequence);

CREATE TABLE idempotency_records (
    id UUID PRIMARY KEY,
    scope VARCHAR(50) NOT NULL,
    idempotency_key VARCHAR(100) NOT NULL,
    request_fingerprint VARCHAR(64) NOT NULL,
    transfer_id UUID NOT NULL,
    http_status INTEGER NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT fk_idempotency_transfer FOREIGN KEY (transfer_id) REFERENCES transfers (id) ON DELETE RESTRICT,
    CONSTRAINT uk_idempotency_scope_key UNIQUE (scope, idempotency_key),
    CONSTRAINT ck_idempotency_scope_not_blank CHECK (length(btrim(scope)) > 0),
    CONSTRAINT ck_idempotency_key_not_blank CHECK (length(btrim(idempotency_key)) > 0),
    CONSTRAINT ck_idempotency_fingerprint_sha256 CHECK (request_fingerprint ~ '^[0-9a-f]{64}$'),
    CONSTRAINT ck_idempotency_http_status CHECK (http_status BETWEEN 100 AND 599)
);

CREATE INDEX ix_idempotency_transfer ON idempotency_records (transfer_id);

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
    CONSTRAINT fk_outbox_transfer FOREIGN KEY (aggregate_id) REFERENCES transfers (id) ON DELETE RESTRICT,
    CONSTRAINT ck_outbox_aggregate_type CHECK (aggregate_type = 'TRANSFER'),
    CONSTRAINT ck_outbox_event_type_not_blank CHECK (length(btrim(event_type)) > 0),
    CONSTRAINT ck_outbox_event_version_positive CHECK (event_version > 0),
    CONSTRAINT ck_outbox_status CHECK (status IN ('PENDING', 'PUBLISHED', 'FAILED')),
    CONSTRAINT ck_outbox_attempts_non_negative CHECK (publish_attempt_count >= 0),
    CONSTRAINT ck_outbox_published_consistency CHECK (
        (status = 'PUBLISHED' AND published_at IS NOT NULL) OR (status <> 'PUBLISHED' AND published_at IS NULL)
    )
);

CREATE INDEX ix_outbox_pending_poll
    ON outbox_events (occurred_at, event_id)
    WHERE status = 'PENDING';
