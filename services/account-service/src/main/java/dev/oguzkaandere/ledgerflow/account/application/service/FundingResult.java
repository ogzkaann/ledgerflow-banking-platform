package dev.oguzkaandere.ledgerflow.account.application.service;

import dev.oguzkaandere.ledgerflow.account.domain.model.Account;
import dev.oguzkaandere.ledgerflow.account.domain.model.LedgerEntry;

public record FundingResult(Account account, LedgerEntry ledgerEntry) {}
