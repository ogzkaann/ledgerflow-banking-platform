package dev.oguzkaandere.ledgerflow.account.application.command;

import dev.oguzkaandere.ledgerflow.account.domain.model.AccountId;
import java.math.BigDecimal;

public record FundAccountCommand(AccountId accountId, BigDecimal amount, String reference) {}
