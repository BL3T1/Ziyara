package com.ziyarah.domain.repository;

import com.ziyarah.domain.entity.Payment;
import com.ziyarah.domain.enums.PaymentStatus;
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
    List<Payment> findByStatus(PaymentStatus status);
    List<Payment> findAll();
    void deleteById(UUID id);
}
