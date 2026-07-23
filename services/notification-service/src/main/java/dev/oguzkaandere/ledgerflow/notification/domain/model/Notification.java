package dev.oguzkaandere.ledgerflow.notification.domain.model;

import java.time.Instant;
import java.util.UUID;

public record Notification(
        UUID notificationId,
        UUID transferId,
        UUID eventId,
        NotificationType type,
        String finalTransferStatus,
        String correlationId,
        String messageTemplateKey,
        Instant createdAt) {
    public Notification {
        if (notificationId == null
                || transferId == null
                || eventId == null
                || type == null
                || !java.util.Set.of("COMPLETED", "REJECTED").contains(finalTransferStatus)
                || correlationId == null
                || correlationId.isBlank()
                || messageTemplateKey == null
                || messageTemplateKey.isBlank()
                || createdAt == null) {
            throw new IllegalArgumentException("Invalid notification");
        }
    }
}
