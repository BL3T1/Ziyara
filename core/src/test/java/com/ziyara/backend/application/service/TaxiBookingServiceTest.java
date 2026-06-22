package com.ziyara.backend.application.service;

import com.ziyara.backend.application.dto.request.AddTaxiRequest;
import com.ziyara.backend.application.dto.response.TaxiBookingResponse;
import com.ziyara.backend.application.exception.ResourceNotFoundException;
import com.ziyara.backend.application.exception.UnauthorizedException;
import com.ziyara.backend.domain.entity.TaxiBooking;
import com.ziyara.backend.domain.enums.TaxiStatus;
import com.ziyara.backend.domain.enums.VehicleType;
import com.ziyara.backend.domain.repository.BookingRepository;
import com.ziyara.backend.domain.repository.TaxiBookingRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TaxiBookingServiceTest {

    @Mock TaxiBookingRepository taxiBookingRepository;
    @Mock BookingRepository bookingRepository;

    @InjectMocks TaxiBookingService taxiBookingService;

    private static final UUID TAXI_ID = UUID.randomUUID();
    private static final UUID BOOKING_ID = UUID.randomUUID();
    private static final UUID DRIVER_ID = UUID.randomUUID();

    private TaxiBooking taxiBooking;

    @BeforeEach
    void setUp() {
        taxiBooking = new TaxiBooking();
        taxiBooking.setId(TAXI_ID);
        taxiBooking.setBookingId(BOOKING_ID);
        taxiBooking.setVehicleType(VehicleType.STANDARD);
        taxiBooking.setPickupLocation("Airport");
        taxiBooking.setDestinationLocation("Hotel");
        taxiBooking.setStatus(TaxiStatus.SEARCHING);
        taxiBooking.setEstimatedPrice(new BigDecimal("50.00"));
    }

    // ── updateTaxiStatus ──────────────────────────────────────────────────────

    @Nested
    class UpdateTaxiStatus {

        @Test
        void inProgress_setsStartedAt() {
            when(taxiBookingRepository.findById(TAXI_ID)).thenReturn(Optional.of(taxiBooking));
            when(taxiBookingRepository.save(any())).thenReturn(taxiBooking);

            TaxiBookingResponse result = taxiBookingService.updateTaxiStatus(TAXI_ID, TaxiStatus.IN_PROGRESS);

            assertThat(taxiBooking.getStatus()).isEqualTo(TaxiStatus.IN_PROGRESS);
            assertThat(taxiBooking.getStartedAt()).isNotNull();
        }

        @Test
        void completed_setsCompletedAt() {
            when(taxiBookingRepository.findById(TAXI_ID)).thenReturn(Optional.of(taxiBooking));
            when(taxiBookingRepository.save(any())).thenReturn(taxiBooking);

            taxiBookingService.updateTaxiStatus(TAXI_ID, TaxiStatus.COMPLETED);

            assertThat(taxiBooking.getStatus()).isEqualTo(TaxiStatus.COMPLETED);
            assertThat(taxiBooking.getCompletedAt()).isNotNull();
        }

        @Test
        void notFound_throws() {
            when(taxiBookingRepository.findById(any())).thenReturn(Optional.empty());

            assertThatThrownBy(() -> taxiBookingService.updateTaxiStatus(TAXI_ID, TaxiStatus.CANCELLED))
                    .isInstanceOf(RuntimeException.class);
        }
    }

    // ── assignDriver ──────────────────────────────────────────────────────────

    @Test
    void assignDriver_setsDriverAndStatus() {
        when(taxiBookingRepository.findById(TAXI_ID)).thenReturn(Optional.of(taxiBooking));
        when(taxiBookingRepository.save(any())).thenReturn(taxiBooking);

        taxiBookingService.assignDriver(TAXI_ID, DRIVER_ID, "John Driver", "ABC-123", "Toyota");

        assertThat(taxiBooking.getDriverId()).isEqualTo(DRIVER_ID);
        assertThat(taxiBooking.getDriverName()).isEqualTo("John Driver");
        assertThat(taxiBooking.getLicensePlate()).isEqualTo("ABC-123");
        assertThat(taxiBooking.getVehicleModel()).isEqualTo("Toyota");
        assertThat(taxiBooking.getStatus()).isEqualTo(TaxiStatus.ASSIGNED);
    }

    // ── getActiveBookings ─────────────────────────────────────────────────────

    @Test
    void getActiveBookings_returnsAssignedBookings() {
        when(taxiBookingRepository.findByStatus(TaxiStatus.ASSIGNED)).thenReturn(List.of(taxiBooking));

        List<TaxiBookingResponse> result = taxiBookingService.getActiveBookings();

        assertThat(result).hasSize(1);
    }

    // ── getTaxiBooking ────────────────────────────────────────────────────────

    @Nested
    class GetTaxiBooking {

        @Test
        void found_returnsResponse() {
            when(taxiBookingRepository.findById(TAXI_ID)).thenReturn(Optional.of(taxiBooking));

            TaxiBookingResponse result = taxiBookingService.getTaxiBooking(TAXI_ID);

            assertThat(result.getId()).isEqualTo(TAXI_ID);
            assertThat(result.getBookingId()).isEqualTo(BOOKING_ID);
        }

        @Test
        void notFound_throwsResourceNotFound() {
            when(taxiBookingRepository.findById(any())).thenReturn(Optional.empty());

            assertThatThrownBy(() -> taxiBookingService.getTaxiBooking(UUID.randomUUID()))
                    .isInstanceOf(ResourceNotFoundException.class);
        }
    }

    // ── assertIsDriver ────────────────────────────────────────────────────────

    @Nested
    class AssertIsDriver {

        @Test
        void correctDriver_doesNotThrow() {
            taxiBooking.setDriverId(DRIVER_ID);
            when(taxiBookingRepository.findByBookingId(BOOKING_ID)).thenReturn(Optional.of(taxiBooking));

            taxiBookingService.assertIsDriver(BOOKING_ID, DRIVER_ID);
        }

        @Test
        void wrongDriver_throwsUnauthorized() {
            taxiBooking.setDriverId(DRIVER_ID);
            when(taxiBookingRepository.findByBookingId(BOOKING_ID)).thenReturn(Optional.of(taxiBooking));

            assertThatThrownBy(() -> taxiBookingService.assertIsDriver(BOOKING_ID, UUID.randomUUID()))
                    .isInstanceOf(UnauthorizedException.class);
        }

        @Test
        void noCrTaxiForBooking_throwsResourceNotFound() {
            when(taxiBookingRepository.findByBookingId(BOOKING_ID)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> taxiBookingService.assertIsDriver(BOOKING_ID, DRIVER_ID))
                    .isInstanceOf(ResourceNotFoundException.class);
        }
    }
}
