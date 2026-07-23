package dev.oguzkaandere.ledgerflow.transfer.domain.model;

import dev.oguzkaandere.ledgerflow.transfer.domain.exception.InvalidTransferException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Objects;

public record Money(BigDecimal amount, SupportedCurrency currency) {
    public static final int PRECISION = 19;
    public static final int SCALE = 2;

    public Money {
        Objects.requireNonNull(amount, "amount is required");
        Objects.requireNonNull(currency, "currency is required");
        if (amount.scale() > SCALE) {
            throw new InvalidTransferException("Amount must have at most two decimal places");
        }
        if (amount.signum() <= 0) {
            throw new InvalidTransferException("Amount must be greater than zero");
        }
        if (amount.precision() + Math.max(0, SCALE - amount.scale()) > PRECISION) {
            throw new InvalidTransferException("Amount exceeds the supported precision");
        }
        amount = amount.setScale(SCALE, RoundingMode.UNNECESSARY);
    }

    public String canonicalAmount() {
        return amount.toPlainString();
    }
}
