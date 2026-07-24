package dev.oguzkaandere.ledgerflow.notification.adapter.in.web;

import dev.oguzkaandere.ledgerflow.notification.application.service.NotificationWorkflowService;
import dev.oguzkaandere.ledgerflow.notification.domain.model.Notification;
import dev.oguzkaandere.ledgerflow.notification.domain.model.NotificationSearchCriteria;
import dev.oguzkaandere.ledgerflow.notification.domain.model.NotificationType;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.context.annotation.Profile;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Validated
@Profile({"local", "test"})
@RequestMapping("/api/v1/notifications")
class NotificationController {
    private final NotificationWorkflowService notifications;

    NotificationController(NotificationWorkflowService notifications) {
        this.notifications = notifications;
    }

    @GetMapping
    NotificationPageResponse list(
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size,
            @RequestParam(required = false) UUID transferId,
            @RequestParam(required = false) NotificationType type) {
        var result = notifications.list(new NotificationSearchCriteria(page, size, transferId, type));
        return new NotificationPageResponse(
                result.content().stream()
                        .map(NotificationController::toResponse)
                        .toList(),
                result.page(),
                result.size(),
                result.totalElements(),
                result.totalPages());
    }

    private static NotificationResponse toResponse(Notification value) {
        return new NotificationResponse(
                value.notificationId(),
                value.transferId(),
                value.eventId(),
                value.type().name(),
                value.finalTransferStatus(),
                value.correlationId(),
                value.messageTemplateKey(),
                value.createdAt());
    }

    record NotificationPageResponse(
            List<NotificationResponse> content, int page, int size, long totalElements, int totalPages) {}

    record NotificationResponse(
            UUID notificationId,
            UUID transferId,
            UUID eventId,
            String type,
            String finalTransferStatus,
            String correlationId,
            String messageTemplateKey,
            Instant createdAt) {}
}
