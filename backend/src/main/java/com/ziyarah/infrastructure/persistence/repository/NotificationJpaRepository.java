package com.ziyarah.infrastructure.persistence.repository;

import com.ziyarah.domain.enums.NotificationStatus;
import com.ziyarah.infrastructure.persistence.entity.NotificationJpaEntity;
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
    List<NotificationJpaEntity> findByUserIdAndStatus(UUID userId, NotificationStatus status);
    List<NotificationJpaEntity> findByStatus(NotificationStatus status);
    long countByUserIdAndStatus(UUID userId, NotificationStatus status);
}
