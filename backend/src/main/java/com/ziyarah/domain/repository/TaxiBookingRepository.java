package com.ziyarah.domain.repository;

import com.ziyarah.domain.entity.TaxiBooking;
import com.ziyarah.domain.enums.TaxiStatus;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface TaxiBookingRepository {
    TaxiBooking save(TaxiBooking taxiBooking);
    Optional<TaxiBooking> findById(UUID id);
    Optional<TaxiBooking> findByBookingId(UUID bookingId);
    List<TaxiBooking> findByStatus(TaxiStatus status);
}
