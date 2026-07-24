package dev.oguzkaandere.ledgerflow.transfer.domain.model;

import java.time.Instant;
import java.util.UUID;

public record TransferSearchCriteria(
        int page,
        int size,
        TransferStatus status,
        UUID sourceAccountId,
        UUID destinationAccountId,
        String reference,
        String correlationId,
        Instant createdFrom,
        Instant createdTo) {

    public TransferSearchCriteria {
        if (page < 0 || size < 1 || size > 100) {
            throw new IllegalArgumentException("Page must be non-negative and size must be between 1 and 100");
        }
        reference = normalize(reference, "Reference");
        correlationId = normalize(correlationId, "Correlation ID");
        if (createdFrom != null && createdTo != null && createdFrom.isAfter(createdTo)) {
            throw new IllegalArgumentException("createdFrom must not be after createdTo");
        }
    }

    private static String normalize(String value, String label) {
        if (value == null) {
            return null;
        }
        String normalized = value.trim();
        if (normalized.isEmpty()) {
            return null;
        }
        if (normalized.length() > 100) {
            throw new IllegalArgumentException(label + " must not exceed 100 characters");
        }
        return normalized;
    }
}
