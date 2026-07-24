CREATE INDEX ix_notifications_operations_order
    ON notifications (created_at DESC, notification_id DESC);

CREATE INDEX ix_notifications_type_operations_order
    ON notifications (type, created_at DESC, notification_id DESC);
