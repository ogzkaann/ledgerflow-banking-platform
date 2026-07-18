package dev.oguzkaandere.ledgerflow.account.adapter.in.web;

import dev.oguzkaandere.ledgerflow.account.adapter.in.web.dto.AccountResponse;
import dev.oguzkaandere.ledgerflow.account.adapter.in.web.dto.FundingResponse;
import dev.oguzkaandere.ledgerflow.account.adapter.in.web.dto.LedgerEntryResponse;
import dev.oguzkaandere.ledgerflow.account.adapter.in.web.dto.LedgerPageResponse;
import dev.oguzkaandere.ledgerflow.account.application.service.FundingResult;
import dev.oguzkaandere.ledgerflow.account.domain.model.Account;
import dev.oguzkaandere.ledgerflow.account.domain.model.LedgerEntry;
import dev.oguzkaandere.ledgerflow.account.domain.model.LedgerPage;

final class AccountWebMapper {

    private AccountWebMapper() {}

    static AccountResponse toResponse(Account account) {
        return new AccountResponse(
                account.id().value(),
                account.ownerReference(),
                account.currency().name(),
                account.status().name(),
                account.availableBalance().formattedAmount(),
                account.reservedBalance().formattedAmount(),
                account.version(),
                account.createdAt(),
                account.updatedAt());
    }

    static LedgerEntryResponse toResponse(LedgerEntry entry) {
        return new LedgerEntryResponse(
                entry.ledgerEntryId(),
                entry.accountId().value(),
                entry.type().name(),
                entry.amount().formattedAmount(),
                entry.amount().currency().name(),
                entry.reference().value(),
                entry.createdAt());
    }

    static LedgerPageResponse toResponse(LedgerPage page) {
        return new LedgerPageResponse(
                page.content().stream().map(AccountWebMapper::toResponse).toList(),
                page.page(),
                page.size(),
                page.totalElements(),
                page.totalPages());
    }

    static FundingResponse toResponse(FundingResult result) {
        return new FundingResponse(toResponse(result.account()), toResponse(result.ledgerEntry()));
    }
}
