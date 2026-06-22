package com.ziyara.backend.application.service;

import com.ziyara.backend.application.exception.BusinessException;
import com.ziyara.backend.application.exception.ResourceNotFoundException;
import com.ziyara.backend.application.exception.UnauthorizedException;
import com.ziyara.backend.domain.entity.Booking;
import com.ziyara.backend.domain.enums.BookingStatus;
import com.ziyara.backend.domain.repository.BookingRepository;
import com.ziyara.backend.domain.repository.DiscountCodeRepository;
import com.ziyara.backend.domain.repository.ServiceRepository;
import com.ziyara.backend.domain.repository.UserRepository;
import com.ziyara.backend.infrastructure.messaging.StaffNotificationCommandPublisher;
import com.ziyara.backend.modules.pricing.api.PricingEngineApi;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BookingServiceTest {

    @Mock BookingRepository bookingRepository;
    @Mock ServiceRepository serviceRepository;
    @Mock UserRepository userRepository;
    @Mock DiscountCodeRepository discountCodeRepository;
    @Mock PricingEngineApi pricingService;
    @Mock TaxiBookingService taxiBookingService;
    @Mock StaffNotificationCommandPublisher staffNotificationCommandPublisher;

    @InjectMocks BookingService bookingService;

    private static final UUID CUSTOMER_A = UUID.fromString("a0000000-0000-0000-0000-000000000001");
    private static final UUID CUSTOMER_B = UUID.fromString("b0000000-0000-0000-0000-000000000002");
    private static final UUID BOOKING_ID = UUID.fromString("c0000000-0000-0000-0000-000000000003");
    private static final UUID SERVICE_ID = UUID.fromString("d0000000-0000-0000-0000-000000000004");

    // ── getBookingById ────────────────────────────────────────────────────────

    @Test
    void getBookingById_notFound_throwsResourceNotFound() {
        when(bookingRepository.findById(BOOKING_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> bookingService.getBookingById(BOOKING_ID, CUSTOMER_A, false))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Booking not found");
    }

    @Test
    void getBookingById_ownerAccess_returnsResponse() {
        Booking booking = pendingBooking(BOOKING_ID, CUSTOMER_A, SERVICE_ID);
        when(bookingRepository.findById(BOOKING_ID)).thenReturn(Optional.of(booking));

        var response = bookingService.getBookingById(BOOKING_ID, CUSTOMER_A, false);

        assertThat(response.getId()).isEqualTo(BOOKING_ID);
        assertThat(response.getCustomerId()).isEqualTo(CUSTOMER_A);
    }

    @Test
    void getBookingById_differentCustomer_throwsUnauthorized() {
        Booking booking = pendingBooking(BOOKING_ID, CUSTOMER_A, SERVICE_ID);
        when(bookingRepository.findById(BOOKING_ID)).thenReturn(Optional.of(booking));

        assertThatThrownBy(() -> bookingService.getBookingById(BOOKING_ID, CUSTOMER_B, false))
                .isInstanceOf(UnauthorizedException.class)
                .hasMessageContaining("access to this booking");
    }

    @Test
    void getBookingById_companyStaffCanAccessAnyBooking() {
        Booking booking = pendingBooking(BOOKING_ID, CUSTOMER_A, SERVICE_ID);
        when(bookingRepository.findById(BOOKING_ID)).thenReturn(Optional.of(booking));

        var response = bookingService.getBookingById(BOOKING_ID, CUSTOMER_B, true);

        assertThat(response.getId()).isEqualTo(BOOKING_ID);
    }

    // ── cancelBooking ─────────────────────────────────────────────────────────

    @Test
    void cancelBooking_differentCustomer_throwsUnauthorized() {
        Booking booking = pendingBooking(BOOKING_ID, CUSTOMER_A, SERVICE_ID);
        when(bookingRepository.findById(BOOKING_ID)).thenReturn(Optional.of(booking));

        assertThatThrownBy(() ->
                bookingService.cancelBooking(BOOKING_ID, CUSTOMER_B, false, "changed mind"))
                .isInstanceOf(UnauthorizedException.class);
    }

    @Test
    void cancelBooking_notFound_throwsResourceNotFound() {
        when(bookingRepository.findById(BOOKING_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() ->
                bookingService.cancelBooking(BOOKING_ID, CUSTOMER_A, false, "changed mind"))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    // ── createBooking — conflict check ────────────────────────────────────────

    @Test
    void createBooking_conflictingDates_throwsBusinessException() {
        when(bookingRepository.hasConflictingBooking(
                SERVICE_ID,
                LocalDate.of(2026, 6, 1),
                LocalDate.of(2026, 6, 5)))
                .thenReturn(true);

        var request = new com.ziyara.backend.application.dto.BookingRequest();
        request.setServiceId(SERVICE_ID);
        request.setCheckInDate(LocalDate.of(2026, 6, 1));
        request.setCheckOutDate(LocalDate.of(2026, 6, 5));

        assertThatThrownBy(() -> bookingService.createBooking(CUSTOMER_A, request))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("not available for the selected dates");
    }

    // ── helpers ───────────────────────────────────────────────────────────────

    private static Booking pendingBooking(UUID id, UUID customerId, UUID serviceId) {
        Booking b = new Booking();
        b.setId(id);
        b.setCustomerId(customerId);
        b.setServiceId(serviceId);
        b.setStatus(BookingStatus.PENDING);
        b.setTotalAmount(BigDecimal.valueOf(100));
        b.setCurrency("USD");
        return b;
    }
}
