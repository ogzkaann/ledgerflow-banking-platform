CREATE TABLE risk_decisions (
    decision_id UUID PRIMARY KEY,
    transfer_id UUID NOT NULL UNIQUE,
    amount NUMERIC(19,2) NOT NULL,
    currency VARCHAR(3) NOT NULL,
    reference VARCHAR(100) NOT NULL,
    outcome VARCHAR(20) NOT NULL,
    reason VARCHAR(50) NOT NULL,
    rule_version VARCHAR(50) NOT NULL,
    correlation_id VARCHAR(100) NOT NULL,
    decided_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT ck_risk_amount_positive CHECK (amount > 0),
    CONSTRAINT ck_risk_currency CHECK (currency IN ('EUR','USD','GBP')),
    CONSTRAINT ck_risk_outcome CHECK (outcome IN ('APPROVED','REJECTED')),
    CONSTRAINT ck_risk_reference CHECK (length(btrim(reference)) > 0),
    CONSTRAINT ck_risk_reason CHECK (length(btrim(reason)) > 0),
    CONSTRAINT ck_risk_rule_version CHECK (length(btrim(rule_version)) > 0),
    CONSTRAINT ck_risk_correlation CHECK (length(btrim(correlation_id)) > 0)
);

CREATE TABLE processed_events (
    event_id UUID PRIMARY KEY,
    event_type VARCHAR(100) NOT NULL,
    processed_at TIMESTAMPTZ NOT NULL
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
    CONSTRAINT ck_risk_outbox_version CHECK (event_version > 0),
    CONSTRAINT ck_risk_outbox_status CHECK (status IN ('PENDING','PUBLISHED','FAILED')),
    CONSTRAINT ck_risk_outbox_attempts CHECK (publish_attempt_count >= 0),
    CONSTRAINT ck_risk_outbox_published CHECK (
        (status = 'PUBLISHED' AND published_at IS NOT NULL)
        OR (status <> 'PUBLISHED' AND published_at IS NULL)
    )
);

CREATE INDEX ix_risk_outbox_pending ON outbox_events (status, occurred_at, event_id);
