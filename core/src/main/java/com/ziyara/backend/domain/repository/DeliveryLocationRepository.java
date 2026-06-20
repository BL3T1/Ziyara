package com.ziyara.backend.domain.repository;

import com.ziyara.backend.domain.entity.DeliveryLocation;

import java.util.Optional;
import java.util.UUID;

public interface DeliveryLocationRepository {
    Optional<DeliveryLocation> findLatestByBookingId(UUID bookingId);
}
