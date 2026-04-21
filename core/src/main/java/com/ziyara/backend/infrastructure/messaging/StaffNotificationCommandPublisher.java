package com.ziyara.backend.infrastructure.messaging;

/**
 * Publishes staff notification commands (Kafka when enabled; no-op otherwise).
 */
public interface StaffNotificationCommandPublisher {

    void publishAfterCommit(StaffNotificationEvent event);
}
