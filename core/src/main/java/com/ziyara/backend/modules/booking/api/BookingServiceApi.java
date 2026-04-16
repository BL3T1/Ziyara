package com.ziyara.backend.modules.booking.api;

import com.ziyara.backend.application.dto.BookingResponse;

import java.util.Optional;
import java.util.UUID;

/**
 * Booking module API (Phase 3). Other modules (e.g. payment) depend on this to resolve bookings by ID.
 */
public interface BookingServiceApi {

    /**
     * Get a booking by ID. Returns empty if not found or not accessible.
     */
    Optional<BookingResponse> getBooking(UUID bookingId);
}
