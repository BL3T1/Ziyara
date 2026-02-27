package com.ziyarah.infrastructure.persistence.repository;

import com.ziyarah.domain.enums.RefundStatus;
import com.ziyarah.infrastructure.persistence.entity.RefundJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.UUID;

/**
 * Spring Data JPA Repository: RefundJpaRepository
 */
@Repository
public interface RefundJpaRepository extends JpaRepository<RefundJpaEntity, UUID> {
    List<RefundJpaEntity> findByPaymentId(UUID paymentId);
    List<RefundJpaEntity> findByStatus(RefundStatus status);
}
