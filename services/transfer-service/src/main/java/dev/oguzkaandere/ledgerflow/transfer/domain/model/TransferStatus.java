package dev.oguzkaandere.ledgerflow.transfer.domain.model;

public enum TransferStatus {
    PENDING,
    FUNDS_RESERVED,
    RISK_APPROVED,
    SETTLING,
    COMPLETED,
    REJECTED,
    COMPENSATING,
    EXPIRED;

    public boolean terminal() {
        return this == COMPLETED || this == REJECTED || this == EXPIRED;
    }
}
