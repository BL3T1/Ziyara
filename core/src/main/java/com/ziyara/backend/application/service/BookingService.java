package com.ziyara.backend.application.service;

import com.ziyara.backend.application.dto.BookingResponse;
import com.ziyara.backend.domain.entity.Booking;
import com.ziyara.backend.domain.enums.BookingStatus;
import com.ziyara.backend.domain.repository.BookingRepository;
import com.ziyara.backend.modules.booking.api.BookingServiceApi;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.UUID;

/**
 * Booking operations – implements BookingServiceApi (Phase 3). Other modules use this API to resolve bookings.
 */
@Service
@RequiredArgsConstructor
public class BookingService implements BookingServiceApi {

    private final BookingRepository bookingRepository;

    @Override
    @Transactional(readOnly = true)
    public Optional<BookingResponse> getBooking(UUID bookingId) {
        return bookingRepository.findById(bookingId).map(this::toResponse);
    }

    @Transactional(readOnly = true)
    public Page<BookingResponse> pageForCustomer(UUID customerUserId, @Nullable BookingStatus status, int page, int size) {
        int p = Math.max(0, page);
        int s = Math.min(100, Math.max(1, size));
        PageRequest pr = PageRequest.of(p, s, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<Booking> bookings = status != null
                ? bookingRepository.findByCustomerIdAndStatus(customerUserId, status, pr)
                : bookingRepository.findByCustomerId(customerUserId, pr);
        return bookings.map(this::toResponse);
    }

    private BookingResponse toResponse(Booking b) {
        return BookingResponse.builder()
                .id(b.getId())
                .bookingReference(b.getBookingReference())
                .customerId(b.getCustomerId())
                .serviceId(b.getServiceId())
                .checkInDate(b.getCheckInDate())
                .checkOutDate(b.getCheckOutDate())
                .guests(b.getGuests())
                .rooms(b.getRooms())
                .baseAmount(b.getBaseAmount())
                .discountAmount(b.getDiscountAmount())
                .taxAmount(b.getTaxAmount())
                .commissionAmount(b.getCommissionAmount())
                .totalAmount(b.getTotalAmount())
                .currency(b.getCurrency())
                .status(b.getStatus())
                .specialRequests(b.getSpecialRequests())
                .idDocumentVerified(b.isIdDocumentVerified())
                .confirmedAt(b.getConfirmedAt())
                .cancelledAt(b.getCancelledAt())
                .cancellationReason(b.getCancellationReason())
                .createdAt(b.getCreatedAt())
                .canBeCancelled(b.canBeCancelled())
                .canBeModified(b.canBeModified())
                .build();
    }
}
