package dev.oguzkaandere.ledgerflow.account.domain.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import dev.oguzkaandere.ledgerflow.account.domain.exception.AccountStateException;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class AccountTest {

    private static final Instant CREATED_AT = Instant.parse("2026-07-18T12:00:00Z");

    @Test
    void creditsAnActiveAccountAndPreservesReservedBalance() {
        Account account =
                Account.create(AccountId.from(UUID.randomUUID()), "customer-001", SupportedCurrency.EUR, CREATED_AT);

        Account credited = account.credit(
                Money.positive(new BigDecimal("25.50"), SupportedCurrency.EUR), CREATED_AT.plusSeconds(1));

        assertThat(credited.availableBalance().amount()).isEqualByComparingTo("25.50");
        assertThat(credited.reservedBalance().amount()).isEqualByComparingTo("0.00");
        assertThat(credited.updatedAt()).isEqualTo(CREATED_AT.plusSeconds(1));
    }

    @Test
    void rejectsCreditWhenAccountStatusDoesNotAllowMutation() {
        Account frozen = accountWithStatus(AccountStatus.FROZEN);

        assertThatThrownBy(() -> frozen.credit(
                        Money.positive(new BigDecimal("10.00"), SupportedCurrency.EUR), CREATED_AT.plusSeconds(1)))
                .isInstanceOf(AccountStateException.class)
                .hasMessageContaining("FROZEN");
    }

    private static Account accountWithStatus(AccountStatus status) {
        return new Account(
                AccountId.from(UUID.randomUUID()),
                "customer-001",
                SupportedCurrency.EUR,
                status,
                Money.zero(SupportedCurrency.EUR),
                Money.zero(SupportedCurrency.EUR),
                0,
                CREATED_AT,
                CREATED_AT);
    }
}
