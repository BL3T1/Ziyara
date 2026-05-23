package com.ziyara.backend.application.service;

import com.ziyara.backend.application.dto.request.CreateNotificationRequest;
import com.ziyara.backend.application.dto.response.NotificationInboxResponse;
import com.ziyara.backend.application.dto.response.NotificationResponse;
import com.ziyara.backend.domain.entity.Notification;
import com.ziyara.backend.domain.enums.NotificationStatus;
import com.ziyara.backend.domain.repository.NotificationRepository;
import com.ziyara.backend.infrastructure.persistence.mapper.NotificationMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
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
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final NotificationMapper notificationMapper;

    @Transactional
    public NotificationResponse createNotification(CreateNotificationRequest request) {
        log.info("Creating notification for user: {}", request.getUserId());

        Notification notification = new Notification();
        notification.setUserId(request.getUserId());
        notification.setType(request.getType());
        notification.setChannel(request.getChannel());
        notification.setTitle(request.getTitle());
        notification.setMessage(request.getMessage());
        notification.setTemplateName(request.getTemplateName());
        notification.setMetadata(request.getMetadata());
        notification.setStatus(NotificationStatus.PENDING);

        Notification saved = notificationRepository.save(notification);
        return saved != null ? mapToResponse(saved) : null;
    }

    @Transactional(readOnly = true)
    public List<NotificationResponse> getUserNotifications(UUID userId) {
        return notificationRepository.findByUserId(userId).stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public NotificationInboxResponse getUserNotificationsInbox(UUID userId, int page, int size) {
        int p = Math.max(0, page);
        int s = Math.min(100, Math.max(1, size));
        PageRequest pr = PageRequest.of(p, s, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<NotificationResponse> mapped = notificationRepository
                .findByUserIdOrderByCreatedAtDesc(userId, pr)
                .map(this::mapToResponse);
        long unread = notificationRepository.countByUserIdAndReadAtIsNull(userId);
        return new NotificationInboxResponse(mapped, unread);
    }

    @Transactional(readOnly = true)
    public long countUnread(UUID userId) {
        return notificationRepository.countByUserIdAndReadAtIsNull(userId);
    }

    @Transactional
    public void markAsRead(UUID notificationId) {
        notificationRepository.findById(notificationId).ifPresent(notification -> {
            notification.markAsRead();
            notificationRepository.save(notification);
        });
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
                .build();
    }
}
