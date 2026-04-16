package com.ziyara.backend.application.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.domain.Page;

/**
 * Paginated notifications plus total unread count (for bell badge).
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class NotificationInboxResponse {
    private Page<NotificationResponse> notifications;
    private long unreadCount;
}
