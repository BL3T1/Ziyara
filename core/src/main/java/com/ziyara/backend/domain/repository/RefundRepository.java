package com.ziyara.backend.domain.repository;

import com.ziyara.backend.domain.entity.Refund;
import com.ziyara.backend.domain.enums.RefundStatus;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface RefundRepository {
    Refund save(Refund refund);
    Optional<Refund> findById(UUID id);
    List<Refund> findByPaymentId(UUID paymentId);
    List<Refund> findByBookingId(UUID bookingId);
    List<Refund> findByStatus(RefundStatus status);
    long count();
}
