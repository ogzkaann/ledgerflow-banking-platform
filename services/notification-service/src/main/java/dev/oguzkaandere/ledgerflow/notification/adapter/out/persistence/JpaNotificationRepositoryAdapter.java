package dev.oguzkaandere.ledgerflow.notification.adapter.out.persistence;

import dev.oguzkaandere.ledgerflow.notification.domain.model.Notification;
import dev.oguzkaandere.ledgerflow.notification.domain.model.NotificationType;
import dev.oguzkaandere.ledgerflow.notification.domain.port.NotificationRepository;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
class JpaNotificationRepositoryAdapter implements NotificationRepository {
    private final SpringDataNotificationRepository repository;

    JpaNotificationRepositoryAdapter(SpringDataNotificationRepository repository) {
        this.repository = repository;
    }

    @Override
    public Notification save(Notification notification) {
        return repository
                .saveAndFlush(NotificationJpaEntity.fromDomain(notification))
                .toDomain();
    }

    @Override
    public List<Notification> findByTransferId(UUID transferId) {
        return repository.findByTransferIdOrderByCreatedAtAscNotificationIdAsc(transferId).stream()
                .map(NotificationJpaEntity::toDomain)
                .toList();
    }
}

interface SpringDataNotificationRepository extends JpaRepository<NotificationJpaEntity, UUID> {
    List<NotificationJpaEntity> findByTransferIdOrderByCreatedAtAscNotificationIdAsc(UUID transferId);
}

@Entity
@Table(name = "notifications")
class NotificationJpaEntity {
    @Id
    @Column(name = "notification_id")
    UUID notificationId;

    @Column(name = "transfer_id", nullable = false)
    UUID transferId;

    @Column(name = "event_id", nullable = false, unique = true)
    UUID eventId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    NotificationType type;

    @Column(name = "final_transfer_status", nullable = false, length = 20)
    String finalTransferStatus;

    @Column(name = "correlation_id", nullable = false, length = 100)
    String correlationId;

    @Column(name = "message_template_key", nullable = false, length = 100)
    String messageTemplateKey;

    @Column(name = "created_at", nullable = false)
    Instant createdAt;

    protected NotificationJpaEntity() {}

    static NotificationJpaEntity fromDomain(Notification notification) {
        NotificationJpaEntity entity = new NotificationJpaEntity();
        entity.notificationId = notification.notificationId();
        entity.transferId = notification.transferId();
        entity.eventId = notification.eventId();
        entity.type = notification.type();
        entity.finalTransferStatus = notification.finalTransferStatus();
        entity.correlationId = notification.correlationId();
        entity.messageTemplateKey = notification.messageTemplateKey();
        entity.createdAt = notification.createdAt();
        return entity;
    }

    Notification toDomain() {
        return new Notification(
                notificationId,
                transferId,
                eventId,
                type,
                finalTransferStatus,
                correlationId,
                messageTemplateKey,
                createdAt);
    }
}
