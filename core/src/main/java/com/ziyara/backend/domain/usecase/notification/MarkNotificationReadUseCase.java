package com.ziyara.backend.domain.usecase.notification;

import com.ziyara.backend.domain.entity.Notification;
import com.ziyara.backend.domain.repository.NotificationRepository;

import java.util.Optional;
import java.util.UUID;

public class MarkNotificationReadUseCase {

    private final NotificationRepository notificationRepository;

    public MarkNotificationReadUseCase(NotificationRepository notificationRepository) {
        this.notificationRepository = notificationRepository;
    }

    public Result execute(Input input) {
        Optional<Notification> notifOpt = notificationRepository.findById(input.notificationId());
        if (notifOpt.isEmpty()) {
            return Result.failure("Notification not found");
        }

        Notification notification = notifOpt.get();

        if (!notification.getUserId().equals(input.userId())) {
            return Result.failure("Notification does not belong to this user");
        }

        if (notification.isRead()) {
            return Result.success(notification);
        }

        notification.markAsRead();
        Notification saved = notificationRepository.save(notification);
        return Result.success(saved);
    }

    public record Input(UUID notificationId, UUID userId) {}

    public record Result(boolean success, Notification notification, String error) {
        public static Result success(Notification notification) {
            return new Result(true, notification, null);
        }

        public static Result failure(String error) {
            return new Result(false, null, error);
        }
    }
}
