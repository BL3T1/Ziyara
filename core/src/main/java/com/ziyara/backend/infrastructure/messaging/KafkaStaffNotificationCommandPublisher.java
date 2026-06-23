package com.ziyara.backend.infrastructure.messaging;

import com.ziyara.backend.modules.notification.api.StaffNotificationCommandPublisher;
import com.ziyara.backend.application.dto.StaffNotificationEvent;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "ziyara.notifications.kafka.enabled", havingValue = "true")
@RequiredArgsConstructor
@Slf4j
public class KafkaStaffNotificationCommandPublisher implements StaffNotificationCommandPublisher {

    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;
    private final RunAfterCommit runAfterCommit;

    @Value("${ziyara.notifications.kafka.topic-staff:ziyara.notifications.staff}")
    private String topicStaff;

    @Override
    public void publishAfterCommit(StaffNotificationEvent event) {
        runAfterCommit.execute(() -> {
            try {
                String json = objectMapper.writeValueAsString(event);
                kafkaTemplate.send(topicStaff, event.getEventId().toString(), json);
            } catch (Exception e) {
                log.error("Failed to publish staff notification event {} to Kafka", event.getEventId(), e);
            }
        });
    }
}
