package com.ziyara.backend.modules.booking.api;

import com.ziyara.backend.application.dto.BookingRequest;
import com.ziyara.backend.application.dto.BookingResponse;
import com.ziyara.backend.application.dto.request.AddTaxiRequest;
import com.ziyara.backend.application.dto.request.UpdateBookingRequest;
import com.ziyara.backend.application.dto.response.TaxiBookingResponse;
import com.ziyara.backend.application.dto.response.VoucherResponse;
import com.ziyara.backend.domain.enums.BookingStatus;
import com.ziyara.backend.domain.enums.ServiceType;
import org.springframework.data.domain.Page;

import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

/**
 * Booking module public API.
 *
 * <p>Other modules (e.g. payment) use {@link #getBooking} to resolve bookings by ID.
 * The presentation layer delegates all booking operations through this interface so that
 * no domain repository is imported directly into a controller.
 */
public interface BookingServiceApi {

    /** Used by payment and other modules to resolve a booking by ID. */
    Optional<BookingResponse> getBooking(UUID bookingId);

    // ── Read ─────────────────────────────────────────────────────────────────

    /**
     * Returns bookings visible to the calling user.
     * When {@code scopeAll=true} (company staff only) all bookings are returned;
     * otherwise only bookings owned by {@code userId}.
     */
    Page<BookingResponse> getAllBookings(UUID userId, boolean isCompanyStaff,
                                         BookingStatus status, boolean scopeAll,
                                         int page, int size);

    BookingResponse getBookingById(UUID bookingId, UUID requestingUserId, boolean isCompanyStaff);

    BookingResponse getBookingByReference(String reference, UUID requestingUserId, boolean isCompanyStaff);

    Page<BookingResponse> listForCustomer(UUID customerId, BookingStatus status, int page, int size);

    Page<BookingResponse> listAllAdmin(BookingStatus status, UUID providerId,
                                        ServiceType serviceType,
                                        LocalDate from, LocalDate to,
                                        int page, int size);

    VoucherResponse getVoucher(UUID bookingId, UUID requestingUserId, boolean isCompanyStaff);

    // ── Write ─────────────────────────────────────────────────────────────────

    BookingResponse createBooking(UUID customerId, BookingRequest request);

    BookingResponse updateBooking(UUID id, UpdateBookingRequest request,
                                   UUID requestingUserId, boolean isCompanyStaff);

    BookingResponse confirmBooking(UUID id, UUID requestingUserId, boolean isCompanyStaff);

    BookingResponse rejectBooking(UUID id, UUID requestingUserId, String reason);

    BookingResponse cancelBooking(UUID bookingId, UUID requestingUserId,
                                   boolean isCompanyStaff, String reason);

    TaxiBookingResponse addTaxi(UUID bookingId, AddTaxiRequest request,
                                 UUID requestingUserId, boolean isCompanyStaff);
}
