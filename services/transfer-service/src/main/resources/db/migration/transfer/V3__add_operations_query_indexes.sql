CREATE INDEX ix_transfers_operations_order
    ON transfers (created_at DESC, id DESC);

CREATE INDEX ix_transfers_status_operations_order
    ON transfers (status, created_at DESC, id DESC);

CREATE INDEX ix_transfers_source_operations_order
    ON transfers (source_account_id, created_at DESC, id DESC);

CREATE INDEX ix_transfers_destination_operations_order
    ON transfers (destination_account_id, created_at DESC, id DESC);

CREATE INDEX ix_transfers_correlation
    ON transfers (correlation_id);
