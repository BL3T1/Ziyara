package com.ziyara.backend.infrastructure.messaging;

import com.ziyara.backend.application.dto.request.CreateNotificationRequest;
import com.ziyara.backend.modules.notification.api.NotificationServiceApi;
import com.ziyara.backend.domain.enums.NotificationChannel;
import com.ziyara.backend.domain.enums.NotificationType;
import com.ziyara.backend.infrastructure.persistence.entity.KafkaStaffNotificationDeliveredEntity;
import com.ziyara.backend.infrastructure.persistence.repository.KafkaStaffNotificationDeliveredJpaRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class StaffNotificationInboxProcessor {

    private final StaffNotificationRecipientResolver recipientResolver;
    private final NotificationServiceApi notificationService;
    private final KafkaStaffNotificationDeliveredJpaRepository deliveredRepository;

    @Transactional
    public void process(StaffNotificationEvent event) {
        if (event == null || event.getEventId() == null) {
            log.warn("Staff notification event missing eventId; skip");
            return;
        }
        UUID eventId = event.getEventId();
        Set<UUID> recipients = new LinkedHashSet<>();
        if (event.getRecipientUserId() != null) {
            recipients.add(event.getRecipientUserId());
        }
        recipients.addAll(recipientResolver.resolveRoleNames(event.getNotifyRoles()));

        if (recipients.isEmpty()) {
            log.debug("No recipients for staff notification event {}", eventId);
            return;
        }

        NotificationType type = parseType(event.getNotificationType());
        List<UUID> ordered = new ArrayList<>(recipients);
        for (UUID userId : ordered) {
            if (deliveredRepository.existsDelivery(eventId, userId)) {
                continue;
            }
            String message = event.getMessage() != null && !event.getMessage().isBlank() ? event.getMessage() : "-";
            CreateNotificationRequest req = CreateNotificationRequest.builder()
                    .userId(userId)
                    .type(type)
                    .channel(NotificationChannel.IN_APP)
                    .title(event.getTitle() != null && !event.getTitle().isBlank() ? event.getTitle() : type.name())
                    .message(message)
                    .metadata(event.getMetadata())
                    .build();
            notificationService.createNotification(req);
            deliveredRepository.save(KafkaStaffNotificationDeliveredEntity.builder()
                    .id(new KafkaStaffNotificationDeliveredEntity.Pk(eventId, userId))
                    .createdAt(LocalDateTime.now())
                    .build());
        }
    }

    private static NotificationType parseType(String raw) {
        if (raw == null || raw.isBlank()) {
            return NotificationType.SYSTEM_ALERT;
        }
        try {
            return NotificationType.valueOf(raw.trim());
        } catch (IllegalArgumentException e) {
            return NotificationType.SYSTEM_ALERT;
        }
    }
}
