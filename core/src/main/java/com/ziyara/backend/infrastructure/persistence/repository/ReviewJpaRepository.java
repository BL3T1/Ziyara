package com.ziyara.backend.infrastructure.persistence.repository;

import com.ziyara.backend.domain.enums.ReviewStatus;
import com.ziyara.backend.infrastructure.persistence.entity.ReviewJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Spring Data JPA Repository: ReviewJpaRepository
 */
@Repository
public interface ReviewJpaRepository extends JpaRepository<ReviewJpaEntity, UUID>, JpaSpecificationExecutor<ReviewJpaEntity> {
    Optional<ReviewJpaEntity> findByBookingId(UUID bookingId);
    List<ReviewJpaEntity> findByServiceId(UUID serviceId);
    List<ReviewJpaEntity> findByServiceIdAndStatus(UUID serviceId, ReviewStatus status);
    List<ReviewJpaEntity> findByServiceIdAndStatusIn(UUID serviceId, List<ReviewStatus> statuses);
}
