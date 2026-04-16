package com.ziyara.backend.application.dto.response;

import com.ziyara.backend.domain.enums.NotificationChannel;
import com.ziyara.backend.domain.enums.NotificationStatus;
import com.ziyara.backend.domain.enums.NotificationType;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Notification details")
public class NotificationResponse {
    
    @Schema(description = "Notification ID")
    private UUID id;
    
    @Schema(description = "Target user ID")
    private UUID userId;
    
    @Schema(description = "Notification type")
    private NotificationType type;
    
    @Schema(description = "Notification channel")
    private NotificationChannel channel;
    
    @Schema(description = "Notification status")
    private NotificationStatus status;
    
    @Schema(description = "Notification title")
    private String title;
    
    @Schema(description = "Notification message")
    private String message;
    
    @Schema(description = "Sent timestamp")
    private LocalDateTime sentAt;
    
    @Schema(description = "Read timestamp")
    private LocalDateTime readAt;
    
    @Schema(description = "Creation timestamp")
    private LocalDateTime createdAt;
}
