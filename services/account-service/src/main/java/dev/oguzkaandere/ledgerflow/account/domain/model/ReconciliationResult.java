package dev.oguzkaandere.ledgerflow.account.domain.model;

import java.math.BigDecimal;

public record ReconciliationResult(
        AccountId accountId, BigDecimal ledgerBalance, BigDecimal materializedBalance, boolean balanced) {}
