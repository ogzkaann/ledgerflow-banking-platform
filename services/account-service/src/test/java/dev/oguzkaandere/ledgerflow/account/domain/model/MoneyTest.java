package dev.oguzkaandere.ledgerflow.account.domain.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import dev.oguzkaandere.ledgerflow.account.domain.exception.CurrencyMismatchException;
import dev.oguzkaandere.ledgerflow.account.domain.exception.InvalidMoneyException;
import java.math.BigDecimal;
import org.junit.jupiter.api.Test;

class MoneyTest {

    @Test
    void normalizesValidMoneyToTwoDecimalPlaces() {
        Money money = new Money(new BigDecimal("12.5"), SupportedCurrency.EUR);

        assertThat(money.amount()).isEqualByComparingTo("12.50");
        assertThat(money.formattedAmount()).isEqualTo("12.50");
    }

    @Test
    void rejectsAmountsWithMoreThanTwoDecimalPlaces() {
        assertThatThrownBy(() -> new Money(new BigDecimal("1.001"), SupportedCurrency.EUR))
                .isInstanceOf(InvalidMoneyException.class)
                .hasMessageContaining("at most 2 decimal places");
    }

    @Test
    void positiveMoneyRejectsZeroAndNegativeAmounts() {
        assertThatThrownBy(() -> Money.positive(BigDecimal.ZERO, SupportedCurrency.EUR))
                .isInstanceOf(InvalidMoneyException.class)
                .hasMessageContaining("greater than zero");
        assertThatThrownBy(() -> Money.positive(new BigDecimal("-1.00"), SupportedCurrency.EUR))
                .isInstanceOf(InvalidMoneyException.class)
                .hasMessageContaining("must not be negative");
    }

    @Test
    void rejectsCurrencyMismatchWhenAddingMoney() {
        Money euros = new Money(new BigDecimal("10.00"), SupportedCurrency.EUR);
        Money dollars = new Money(new BigDecimal("5.00"), SupportedCurrency.USD);

        assertThatThrownBy(() -> euros.add(dollars)).isInstanceOf(CurrencyMismatchException.class);
    }
}
