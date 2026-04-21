package com.ziyara.backend.infrastructure.persistence.repository;

import com.ziyara.backend.domain.enums.NotificationStatus;
import com.ziyara.backend.infrastructure.persistence.entity.NotificationJpaEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.UUID;

/**
 * Spring Data JPA Repository: NotificationJpaRepository
 */
@Repository
public interface NotificationJpaRepository extends JpaRepository<NotificationJpaEntity, UUID> {
    List<NotificationJpaEntity> findByUserId(UUID userId);

    Page<NotificationJpaEntity> findByUserIdOrderByCreatedAtDesc(UUID userId, Pageable pageable);

    long countByUserIdAndReadAtIsNull(UUID userId);
    List<NotificationJpaEntity> findByUserIdAndStatus(UUID userId, NotificationStatus status);
    List<NotificationJpaEntity> findByStatus(NotificationStatus status);
    long countByUserIdAndStatus(UUID userId, NotificationStatus status);

    List<NotificationJpaEntity> findTop50ByUserIdOrderByCreatedAtDesc(UUID userId);
}
