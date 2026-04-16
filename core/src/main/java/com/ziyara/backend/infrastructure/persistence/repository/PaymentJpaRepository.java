package com.ziyara.backend.infrastructure.persistence.repository;

import com.ziyara.backend.domain.enums.PaymentStatus;
import com.ziyara.backend.infrastructure.persistence.entity.PaymentJpaEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Spring Data JPA Repository: PaymentJpaRepository
 */
@Repository
public interface PaymentJpaRepository extends JpaRepository<PaymentJpaEntity, UUID> {
    Optional<PaymentJpaEntity> findByBookingId(UUID bookingId);
    Optional<PaymentJpaEntity> findByTransactionRef(String reference);
    Optional<PaymentJpaEntity> findByGatewayReference(String gatewayReference);
    Optional<PaymentJpaEntity> findByIdempotencyKey(String idempotencyKey);
    List<PaymentJpaEntity> findByStatus(PaymentStatus status);

    Page<PaymentJpaEntity> findByStatus(PaymentStatus status, Pageable pageable);

    @Query("SELECT p FROM PaymentJpaEntity p, BookingJpaEntity b WHERE p.bookingId = b.id AND b.customerId = :customerUserId")
    Page<PaymentJpaEntity> findByBookingCustomerUserId(@Param("customerUserId") UUID customerUserId, Pageable pageable);

    @Query("SELECT COALESCE(SUM(p.amount), 0) FROM PaymentJpaEntity p WHERE p.status = :status AND p.createdAt BETWEEN :from AND :to")
    BigDecimal sumCompletedAmountBetween(@Param("status") PaymentStatus status, @Param("from") LocalDateTime from, @Param("to") LocalDateTime to);

    @Query("SELECT COALESCE(SUM(p.amount), 0) FROM PaymentJpaEntity p WHERE p.status = :status AND p.bookingId IN :bookingIds")
    BigDecimal sumCompletedAmountByBookingIds(@Param("status") PaymentStatus status, @Param("bookingIds") List<UUID> bookingIds);
}
