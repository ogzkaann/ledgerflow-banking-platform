CREATE INDEX ix_accounts_operations_order
    ON accounts (created_at DESC, id DESC);

CREATE INDEX ix_accounts_status_currency_order
    ON accounts (status, currency, created_at DESC, id DESC);
