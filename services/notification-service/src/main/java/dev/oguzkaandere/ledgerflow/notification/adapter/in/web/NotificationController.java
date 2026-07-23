package dev.oguzkaandere.ledgerflow.notification.adapter.in.web;

import dev.oguzkaandere.ledgerflow.notification.application.service.NotificationWorkflowService;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.context.annotation.Profile;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Profile({"local", "test"})
@RequestMapping("/api/v1/notifications")
class NotificationController {
    private final NotificationWorkflowService notifications;

    NotificationController(NotificationWorkflowService notifications) {
        this.notifications = notifications;
    }

    @GetMapping
    List<NotificationResponse> findByTransferId(@RequestParam UUID transferId) {
        return notifications.findByTransferId(transferId).stream()
                .map(value -> new NotificationResponse(
                        value.notificationId(),
                        value.transferId(),
                        value.eventId(),
                        value.type().name(),
                        value.finalTransferStatus(),
                        value.correlationId(),
                        value.messageTemplateKey(),
                        value.createdAt()))
                .toList();
    }

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
