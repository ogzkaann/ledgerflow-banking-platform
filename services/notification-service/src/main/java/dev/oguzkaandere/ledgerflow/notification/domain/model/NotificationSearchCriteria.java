package dev.oguzkaandere.ledgerflow.notification.domain.model;

import java.util.UUID;

public record NotificationSearchCriteria(int page, int size, UUID transferId, NotificationType type) {

    public NotificationSearchCriteria {
        if (page < 0 || size < 1 || size > 100) {
            throw new IllegalArgumentException("Page must be non-negative and size must be between 1 and 100");
        }
    }
}
