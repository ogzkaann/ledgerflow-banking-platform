package dev.oguzkaandere.ledgerflow.account.adapter.in.web.dto;

import java.time.Instant;
import java.util.UUID;

public record LedgerEntryResponse(
        UUID ledgerEntryId,
        UUID accountId,
        String type,
        String amount,
        String currency,
        String reference,
        Instant createdAt) {}
