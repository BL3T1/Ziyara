package com.ziyara.backend.domain.repository;

import com.ziyara.backend.domain.entity.TaxiBooking;
import com.ziyara.backend.domain.enums.TaxiStatus;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface TaxiBookingRepository {
    TaxiBooking save(TaxiBooking taxiBooking);
    Optional<TaxiBooking> findById(UUID id);
    Optional<TaxiBooking> findByBookingId(UUID bookingId);
    List<TaxiBooking> findByStatus(TaxiStatus status);
}
