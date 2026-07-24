package dev.oguzkaandere.ledgerflow.notification.domain.model;

import java.util.List;

public record NotificationPage(List<Notification> content, int page, int size, long totalElements, int totalPages) {

    public NotificationPage {
        content = List.copyOf(content);
        if (page < 0 || size < 1 || totalElements < 0 || totalPages < 0) {
            throw new IllegalArgumentException("Invalid notification page metadata");
        }
    }
}
