package com.ziyara.backend.modules.notification.api;

import com.ziyara.backend.application.dto.request.CreateNotificationRequest;
import com.ziyara.backend.application.dto.response.NotificationResponse;

/**
 * Notification module API.
 * Consumers (infrastructure messaging, scheduled jobs) must depend only on this interface.
 */
public interface NotificationServiceApi {

    NotificationResponse createNotification(CreateNotificationRequest request);

    void markAsSent(java.util.UUID notificationId);
}
