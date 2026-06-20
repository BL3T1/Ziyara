package com.ziyara.backend.infrastructure.messaging;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "ziyara.notifications.kafka.enabled", havingValue = "false", matchIfMissing = true)
public class NoOpStaffNotificationCommandPublisher implements StaffNotificationCommandPublisher {

    @Override
    public void publishAfterCommit(StaffNotificationEvent event) {
        // Kafka staff notifications disabled
    }
}
