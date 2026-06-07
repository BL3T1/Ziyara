package com.ziyara.backend.application.service;

import com.ziyara.backend.application.dto.request.CreateNotificationRequest;
import com.ziyara.backend.application.dto.response.NotificationInboxResponse;
import com.ziyara.backend.application.dto.response.NotificationResponse;
import com.ziyara.backend.modules.notification.api.NotificationServiceApi;
import com.ziyara.backend.domain.entity.Notification;
import com.ziyara.backend.domain.enums.NotificationStatus;
import com.ziyara.backend.domain.repository.NotificationRepository;
import com.ziyara.backend.domain.repository.UserRepository;
import com.ziyara.backend.domain.usecase.notification.MarkNotificationReadUseCase;
import com.ziyara.backend.domain.usecase.notification.SendNotificationUseCase;
import com.ziyara.backend.infrastructure.persistence.mapper.NotificationMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import com.ziyara.backend.domain.common.PageQuery;
import com.ziyara.backend.infrastructure.persistence.util.PageConverter;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Service: NotificationService
 * Handles notification delivery and management
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationService implements NotificationServiceApi {

    private final NotificationRepository notificationRepository;
    private final UserRepository userRepository;
    private final NotificationMapper notificationMapper;

    @Transactional
    public NotificationResponse createNotification(CreateNotificationRequest request) {
        log.info("Creating notification for user: {}", request.getUserId());
        var result = new SendNotificationUseCase(notificationRepository, userRepository).execute(
                new SendNotificationUseCase.Input(
                        request.getUserId(), request.getType(), request.getChannel(),
                        request.getTitle(), request.getMessage(),
                        request.getTemplateName(), request.getMetadata()));
        if (!result.success()) {
            log.warn("SendNotificationUseCase failed: {}", result.error());
            return null;
        }
        return mapToResponse(result.notification());
    }

    @Transactional(readOnly = true)
    public List<NotificationResponse> getUserNotifications(UUID userId) {
        return notificationRepository.findByUserId(userId).stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public NotificationInboxResponse getUserNotificationsInbox(UUID userId, int page, int size) {
        PageQuery query = PageQuery.of(Math.max(0, page), Math.min(100, Math.max(1, size)), "createdAt", false);
        Page<NotificationResponse> mapped = PageConverter.toSpringPage(
                notificationRepository.findByUserIdOrderByCreatedAtDesc(userId, query), query, this::mapToResponse);
        long unread = notificationRepository.countByUserIdAndReadAtIsNull(userId);
        return new NotificationInboxResponse(mapped, unread);
    }

    @Transactional(readOnly = true)
    public long countUnread(UUID userId) {
        return notificationRepository.countByUserIdAndReadAtIsNull(userId);
    }

    @Transactional
    public void markAsRead(UUID notificationId, UUID userId) {
        var result = new MarkNotificationReadUseCase(notificationRepository)
                .execute(new MarkNotificationReadUseCase.Input(notificationId, userId));
        if (!result.success()) {
            log.warn("MarkNotificationReadUseCase failed for {}: {}", notificationId, result.error());
        }
    }

    /** Phase 3: Mark all notifications as read for the given user. */
    @Transactional
    public void markAllAsRead(UUID userId) {
        notificationRepository.findByUserId(userId).forEach(notification -> {
            if (!notification.isRead()) {
                notification.markAsRead();
                notificationRepository.save(notification);
            }
        });
    }

    @Transactional(readOnly = true)
    public NotificationResponse getNotification(UUID id, UUID userId) {
        return notificationRepository.findById(id)
                .filter(n -> n.getUserId().equals(userId))
                .map(this::mapToResponse)
                .orElse(null);
    }

    @Transactional
    public void markAsSent(UUID notificationId) {
        notificationRepository.findById(notificationId).ifPresent(notification -> {
            notification.setStatus(NotificationStatus.SENT);
            notification.setSentAt(java.time.LocalDateTime.now());
            notificationRepository.save(notification);
        });
    }

    private NotificationResponse mapToResponse(Notification notification) {
        return NotificationResponse.builder()
                .id(notification.getId())
                .userId(notification.getUserId())
                .type(notification.getType())
                .channel(notification.getChannel())
                .status(notification.getStatus())
                .title(notification.getTitle())
                .message(notification.getMessage())
                .sentAt(notification.getSentAt())
                .readAt(notification.getReadAt())
                .createdAt(notification.getCreatedAt())
                .referenceId(extractReferenceId(notification.getMetadata()))
                .build();
    }

    private String extractReferenceId(String metadata) {
        if (metadata == null || metadata.isBlank()) return null;
        try {
            int keyIdx = metadata.indexOf("\"submissionId\":\"");
            if (keyIdx < 0) return null;
            int start = keyIdx + 16;
            int end = metadata.indexOf('"', start);
            return (end > start) ? metadata.substring(start, end) : null;
        } catch (Exception e) {
            return null;
        }
    }
}
