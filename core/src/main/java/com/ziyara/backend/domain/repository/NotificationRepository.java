package com.ziyara.backend.domain.repository;

import com.ziyara.backend.domain.common.PageQuery;
import com.ziyara.backend.domain.common.PagedResult;
import com.ziyara.backend.domain.entity.Notification;
import com.ziyara.backend.domain.enums.NotificationStatus;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository Port: NotificationRepository
 */
public interface NotificationRepository {
    Notification save(Notification notification);
    Optional<Notification> findById(UUID id);
    List<Notification> findByUserId(UUID userId);

    PagedResult<Notification> findByUserIdOrderByCreatedAtDesc(UUID userId, PageQuery pageQuery);

    long countByUserIdAndReadAtIsNull(UUID userId);
    int markAllReadByUserId(UUID userId);
    List<Notification> findByUserIdAndStatus(UUID userId, NotificationStatus status);
    List<Notification> findByStatus(NotificationStatus status);
    long countByUserIdAndStatus(UUID userId, NotificationStatus status);
    void deleteById(UUID id);
}
