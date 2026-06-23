package com.ziyara.backend.application.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.UUID;

/**
 * JSON payload for the staff notification Kafka topic.
 * Either {@code recipientUserId} or {@code notifyRoles} (UserRole enum names) must be set.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StaffNotificationEvent {

    private UUID eventId;
    private String notificationType;
    private String title;
    private String message;
    private UUID recipientUserId;
    private List<String> notifyRoles;
    private String metadata;
}
