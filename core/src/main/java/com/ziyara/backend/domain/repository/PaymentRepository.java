package com.ziyara.backend.domain.repository;

import com.ziyara.backend.domain.common.PageQuery;
import com.ziyara.backend.domain.common.PagedResult;
import com.ziyara.backend.domain.entity.Payment;
import com.ziyara.backend.domain.enums.PaymentStatus;

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
    List<Payment> findAllByBookingId(UUID bookingId);
    Optional<Payment> findByTransactionReference(String reference);
    Optional<Payment> findByGatewayReference(String gatewayReference);
    Optional<Payment> findByIdempotencyKey(String idempotencyKey);
    List<Payment> findByStatus(PaymentStatus status);

    PagedResult<Payment> findAll(PageQuery pageQuery);

    PagedResult<Payment> findByStatus(PaymentStatus status, PageQuery pageQuery);

    /** Payments whose booking belongs to this customer user id. */
    PagedResult<Payment> findByCustomerUserId(UUID customerUserId, PageQuery pageQuery);

    List<Payment> findAll();
    void deleteById(UUID id);

    BigDecimal sumCompletedAmountBetween(LocalDateTime from, LocalDateTime to);
    BigDecimal sumCompletedAmountByBookingIds(java.util.List<UUID> bookingIds);

    /** Platform-wide sum of payments with the given status (no date filter). */
    BigDecimal sumByStatus(PaymentStatus status);

    /** Completed payments for the given booking IDs on or after {@code since}. Used for weekly chart. */
    List<Payment> findCompletedByBookingIdsSince(List<UUID> bookingIds, LocalDateTime since);
}
