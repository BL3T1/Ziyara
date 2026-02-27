package com.ziyarah.infrastructure.persistence.repository;

import com.ziyarah.domain.enums.ReviewStatus;
import com.ziyarah.infrastructure.persistence.entity.ReviewJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Spring Data JPA Repository: ReviewJpaRepository
 */
@Repository
public interface ReviewJpaRepository extends JpaRepository<ReviewJpaEntity, UUID> {
    Optional<ReviewJpaEntity> findByBookingId(UUID bookingId);
    List<ReviewJpaEntity> findByServiceId(UUID serviceId);
    List<ReviewJpaEntity> findByServiceIdAndStatus(UUID serviceId, ReviewStatus status);
}
