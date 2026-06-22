package com.ziyara.backend.domain.usecase.notification;

import com.ziyara.backend.domain.entity.Notification;
import com.ziyara.backend.domain.enums.NotificationChannel;
import com.ziyara.backend.domain.enums.NotificationType;
import com.ziyara.backend.domain.repository.NotificationRepository;
import com.ziyara.backend.domain.repository.UserRepository;

import java.util.UUID;

public class SendNotificationUseCase {

    private final NotificationRepository notificationRepository;
    private final UserRepository userRepository;

    public SendNotificationUseCase(NotificationRepository notificationRepository, UserRepository userRepository) {
        this.notificationRepository = notificationRepository;
        this.userRepository = userRepository;
    }

    public Result execute(Input input) {
        if (!userRepository.existsById(input.userId())) {
            return Result.failure("User not found");
        }

        if (input.title() == null || input.title().isBlank()) {
            return Result.failure("Notification title is required");
        }

        if (input.message() == null || input.message().isBlank()) {
            return Result.failure("Notification message is required");
        }

        Notification notification = new Notification();
        notification.setUserId(input.userId());
        notification.setType(input.type());
        notification.setChannel(input.channel());
        notification.setTitle(input.title());
        notification.setMessage(input.message());
        notification.setTemplateName(input.templateName());
        notification.setMetadata(input.metadata());
        notification.markAsSent();

        Notification saved = notificationRepository.save(notification);
        return Result.success(saved);
    }

    public record Input(
            UUID userId,
            NotificationType type,
            NotificationChannel channel,
            String title,
            String message,
            String templateName,
            String metadata
    ) {}

    public record Result(boolean success, Notification notification, String error) {
        public static Result success(Notification notification) {
            return new Result(true, notification, null);
        }

        public static Result failure(String error) {
            return new Result(false, null, error);
        }
    }
}
