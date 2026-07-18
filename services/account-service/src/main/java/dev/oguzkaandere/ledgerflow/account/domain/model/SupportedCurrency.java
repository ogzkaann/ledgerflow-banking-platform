package dev.oguzkaandere.ledgerflow.account.domain.model;

import dev.oguzkaandere.ledgerflow.account.domain.exception.UnsupportedCurrencyException;
import java.util.Locale;

public enum SupportedCurrency {
    EUR,
    USD,
    GBP;

    public static SupportedCurrency fromCode(String code) {
        if (code == null || code.isBlank()) {
            throw new UnsupportedCurrencyException(String.valueOf(code));
        }
        try {
            return valueOf(code.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException exception) {
            throw new UnsupportedCurrencyException(code);
        }
    }
}
