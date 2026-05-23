package com.ziyara.backend.domain.repository;

import com.ziyara.backend.domain.entity.Payment;
import com.ziyara.backend.domain.enums.PaymentStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository Port: PaymentRepository
 */
public interface PaymentRepository {
    Payment save(Payment payment);
    Optional<Payment> findById(UUID id);
    Optional<Payment> findByBookingId(UUID bookingId);
    Optional<Payment> findByTransactionReference(String reference);
    Optional<Payment> findByGatewayReference(String gatewayReference);
    Optional<Payment> findByIdempotencyKey(String idempotencyKey);
    List<Payment> findByStatus(PaymentStatus status);

    Page<Payment> findAll(Pageable pageable);

    Page<Payment> findByStatus(PaymentStatus status, Pageable pageable);

    /** Payments whose booking belongs to this customer user id. */
    Page<Payment> findByCustomerUserId(UUID customerUserId, Pageable pageable);

    List<Payment> findAll();
    void deleteById(UUID id);

    BigDecimal sumCompletedAmountBetween(LocalDateTime from, LocalDateTime to);
    BigDecimal sumCompletedAmountByBookingIds(java.util.List<UUID> bookingIds);

    /** Completed payments for the given booking IDs on or after {@code since}. Used for weekly chart. */
    List<Payment> findCompletedByBookingIdsSince(List<UUID> bookingIds, LocalDateTime since);
}
