package dev.oguzkaandere.ledgerflow.transfer.domain.model;

import dev.oguzkaandere.ledgerflow.transfer.domain.exception.InvalidTransferException;
import java.util.Locale;

public enum SupportedCurrency {
    EUR,
    USD,
    GBP;

    public static SupportedCurrency parse(String value) {
        try {
            return valueOf(value == null ? "" : value.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException exception) {
            throw new InvalidTransferException("Currency must be one of EUR, USD, or GBP");
        }
    }
}
