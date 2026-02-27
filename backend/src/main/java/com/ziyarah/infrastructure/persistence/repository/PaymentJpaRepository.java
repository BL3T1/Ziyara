package com.ziyarah.infrastructure.persistence.repository;

import com.ziyarah.domain.enums.PaymentStatus;
import com.ziyarah.infrastructure.persistence.entity.PaymentJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Spring Data JPA Repository: PaymentJpaRepository
 */
@Repository
public interface PaymentJpaRepository extends JpaRepository<PaymentJpaEntity, UUID> {
    Optional<PaymentJpaEntity> findByBookingId(UUID bookingId);
    Optional<PaymentJpaEntity> findByTransactionReference(String reference);
    List<PaymentJpaEntity> findByStatus(PaymentStatus status);
}
