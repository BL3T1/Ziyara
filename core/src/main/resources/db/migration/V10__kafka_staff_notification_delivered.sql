-- Idempotency for Kafka-driven staff notifications (event + recipient)
CREATE TABLE IF NOT EXISTS kafka_staff_notification_delivered (
    event_id UUID NOT NULL,
    user_id UUID NOT NULL,
    created_at TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT (CURRENT_TIMESTAMP),
    PRIMARY KEY (event_id, user_id)
);

CREATE INDEX IF NOT EXISTS idx_kafka_staff_notif_delivered_user ON kafka_staff_notification_delivered (user_id);
