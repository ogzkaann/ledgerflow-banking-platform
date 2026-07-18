package dev.oguzkaandere.ledgerflow.account.domain.model;

import java.math.BigDecimal;

public enum LedgerEntryType {
    CREDIT {
        @Override
        public BigDecimal applySign(BigDecimal amount) {
            return amount;
        }
    },
    DEBIT {
        @Override
        public BigDecimal applySign(BigDecimal amount) {
            return amount.negate();
        }
    };

    public abstract BigDecimal applySign(BigDecimal amount);
}
