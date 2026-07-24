package dev.oguzkaandere.ledgerflow.notification.domain.port;

import dev.oguzkaandere.ledgerflow.notification.domain.model.Notification;
import dev.oguzkaandere.ledgerflow.notification.domain.model.NotificationPage;
import dev.oguzkaandere.ledgerflow.notification.domain.model.NotificationSearchCriteria;
import java.util.List;
import java.util.UUID;

public interface NotificationRepository {
    Notification save(Notification notification);

    List<Notification> findByTransferId(UUID transferId);

    NotificationPage findPage(NotificationSearchCriteria criteria);
}
