package com.ziyara.backend.infrastructure.messaging;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
@ConditionalOnProperty(name = "ziyara.notifications.kafka.enabled", havingValue = "true")
public class StaffNotificationKafkaListener {

    private final ObjectMapper objectMapper;
    private final StaffNotificationInboxProcessor inboxProcessor;

    @KafkaListener(
            topics = "${ziyara.notifications.kafka.topic-staff:ziyara.notifications.staff}",
            groupId = "${ziyara.notifications.kafka.consumer-group:ziyarah-backend-staff-notifications}"
    )
    public void onStaffNotification(
            @Payload String json,
            @Header(KafkaHeaders.RECEIVED_TOPIC) String topic
    ) {
        try {
            StaffNotificationEvent event = objectMapper.readValue(json, StaffNotificationEvent.class);
            inboxProcessor.process(event);
        } catch (Exception e) {
            log.error("Failed to process staff notification from topic {}", topic, e);
            throw new RuntimeException("Staff notification consume failed", e);
        }
    }
}
