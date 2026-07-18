package dev.oguzkaandere.ledgerflow.account.adapter.in.web.dto;

import java.time.Instant;
import java.util.UUID;

public record AccountResponse(
        UUID accountId,
        String ownerReference,
        String currency,
        String status,
        String availableBalance,
        String reservedBalance,
        long version,
        Instant createdAt,
        Instant updatedAt) {}
