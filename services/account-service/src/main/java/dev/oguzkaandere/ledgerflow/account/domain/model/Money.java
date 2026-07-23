package dev.oguzkaandere.ledgerflow.account.domain.model;

import dev.oguzkaandere.ledgerflow.account.domain.exception.CurrencyMismatchException;
import dev.oguzkaandere.ledgerflow.account.domain.exception.InvalidMoneyException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Objects;

public record Money(BigDecimal amount, SupportedCurrency currency) {

    public static final int SCALE = 2;
    public static final int PRECISION = 19;

    public Money {
        Objects.requireNonNull(amount, "Amount is required");
        Objects.requireNonNull(currency, "Currency is required");
        if (amount.scale() > SCALE) {
            throw new InvalidMoneyException("Amount must have at most " + SCALE + " decimal places");
        }
        if (amount.signum() < 0) {
            throw new InvalidMoneyException("Amount must not be negative");
        }
        try {
            amount = amount.setScale(SCALE, RoundingMode.UNNECESSARY);
        } catch (ArithmeticException exception) {
            throw new InvalidMoneyException("Amount must have at most " + SCALE + " decimal places");
        }
        if (amount.precision() > PRECISION) {
            throw new InvalidMoneyException("Amount exceeds the supported precision");
        }
    }

    public static Money zero(SupportedCurrency currency) {
        return new Money(BigDecimal.ZERO, currency);
    }

    public static Money positive(BigDecimal amount, SupportedCurrency currency) {
        Money money = new Money(amount, currency);
        if (money.amount.signum() == 0) {
            throw new InvalidMoneyException("Amount must be greater than zero");
        }
        return money;
    }

    public Money add(Money other) {
        requireSameCurrency(other);
        return new Money(amount.add(other.amount), currency);
    }

    public Money subtract(Money other) {
        requireSameCurrency(other);
        if (amount.compareTo(other.amount) < 0) {
            throw new InvalidMoneyException("Balance must not become negative");
        }
        return new Money(amount.subtract(other.amount), currency);
    }

    public void requireSameCurrency(Money other) {
        Objects.requireNonNull(other, "Money is required");
        if (currency != other.currency) {
            throw new CurrencyMismatchException(currency, other.currency);
        }
    }

    public String formattedAmount() {
        return amount.toPlainString();
    }
}
