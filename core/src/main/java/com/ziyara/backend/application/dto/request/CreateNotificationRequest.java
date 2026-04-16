package com.ziyara.backend.application.dto.request;

import com.ziyara.backend.domain.enums.NotificationChannel;
import com.ziyara.backend.domain.enums.NotificationType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Request to create a notification")
public class CreateNotificationRequest {
    
    @NotNull(message = "User ID is required")
    @Schema(description = "Target user ID")
    private UUID userId;
    
    @NotNull(message = "Notification type is required")
    @Schema(description = "Type of notification")
    private NotificationType type;
    
    @NotNull(message = "Notification channel is required")
    @Schema(description = "Channel for notification")
    private NotificationChannel channel;
    
    @NotBlank(message = "Title is required")
    @Schema(description = "Notification title")
    private String title;
    
    @NotBlank(message = "Message is required")
    @Schema(description = "Notification message content")
    private String message;
    
    @Schema(description = "Optional template name")
    private String templateName;
    
    @Schema(description = "Optional metadata JSON")
    private String metadata;
}
