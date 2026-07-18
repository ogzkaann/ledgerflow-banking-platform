CREATE TABLE accounts (
    id UUID PRIMARY KEY,
    owner_reference VARCHAR(100) NOT NULL,
    currency VARCHAR(3) NOT NULL,
    status VARCHAR(20) NOT NULL,
    available_balance NUMERIC(19, 2) NOT NULL DEFAULT 0.00,
    reserved_balance NUMERIC(19, 2) NOT NULL DEFAULT 0.00,
    version BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT ck_accounts_owner_reference_not_blank CHECK (length(btrim(owner_reference)) > 0),
    CONSTRAINT ck_accounts_currency CHECK (currency IN ('EUR', 'USD', 'GBP')),
    CONSTRAINT ck_accounts_status CHECK (status IN ('ACTIVE', 'FROZEN', 'CLOSED')),
    CONSTRAINT ck_accounts_available_balance_non_negative CHECK (available_balance >= 0),
    CONSTRAINT ck_accounts_reserved_balance_non_negative CHECK (reserved_balance >= 0),
    CONSTRAINT ck_accounts_version_non_negative CHECK (version >= 0)
);

CREATE INDEX ix_accounts_owner_reference ON accounts (owner_reference);

CREATE TABLE ledger_entries (
    id UUID PRIMARY KEY,
    account_id UUID NOT NULL,
    entry_type VARCHAR(20) NOT NULL,
    amount NUMERIC(19, 2) NOT NULL,
    currency VARCHAR(3) NOT NULL,
    reference VARCHAR(100) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT fk_ledger_entries_account FOREIGN KEY (account_id) REFERENCES accounts (id) ON DELETE RESTRICT,
    CONSTRAINT uk_ledger_entries_account_reference UNIQUE (account_id, reference),
    CONSTRAINT ck_ledger_entries_type CHECK (entry_type IN ('CREDIT', 'DEBIT')),
    CONSTRAINT ck_ledger_entries_amount_positive CHECK (amount > 0),
    CONSTRAINT ck_ledger_entries_currency CHECK (currency IN ('EUR', 'USD', 'GBP')),
    CONSTRAINT ck_ledger_entries_reference_not_blank CHECK (length(btrim(reference)) > 0)
);

CREATE INDEX ix_ledger_entries_account_created_at
    ON ledger_entries (account_id, created_at DESC, id DESC);
