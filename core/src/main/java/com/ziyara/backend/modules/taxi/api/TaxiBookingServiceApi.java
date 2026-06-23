package com.ziyara.backend.modules.taxi.api;

import com.ziyara.backend.application.dto.request.AddTaxiRequest;
import com.ziyara.backend.application.dto.response.TaxiBookingResponse;
import com.ziyara.backend.domain.enums.TaxiStatus;

import java.util.List;
import java.util.UUID;

public interface TaxiBookingServiceApi {

    TaxiBookingResponse createForBooking(UUID bookingId, AddTaxiRequest request);

    TaxiBookingResponse updateTaxiStatus(UUID taxiBookingId, TaxiStatus status);

    TaxiBookingResponse assignDriver(UUID taxiBookingId, UUID driverId, String driverName, String plate, String model);

    List<TaxiBookingResponse> getActiveBookings();

    TaxiBookingResponse getTaxiBooking(UUID id);

    void assertIsDriver(UUID bookingId, UUID userId);
}
