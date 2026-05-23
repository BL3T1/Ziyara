package com.ziyara.backend.application.service;

import com.ziyara.backend.application.dto.BookingRequest;
import com.ziyara.backend.application.dto.BookingResponse;
import com.ziyara.backend.application.dto.request.AddTaxiRequest;
import com.ziyara.backend.application.dto.request.PricePreviewRequest;
import com.ziyara.backend.application.dto.request.UpdateBookingRequest;
import com.ziyara.backend.application.dto.response.PriceBreakdownResponse;
import com.ziyara.backend.application.dto.response.TaxiBookingResponse;
import com.ziyara.backend.application.dto.response.VoucherResponse;
import com.ziyara.backend.application.exception.BusinessException;
import com.ziyara.backend.application.exception.ResourceNotFoundException;
import com.ziyara.backend.application.exception.UnauthorizedException;
import com.ziyara.backend.core.api.PricingEngineApi;
import com.ziyara.backend.domain.entity.Booking;
import com.ziyara.backend.domain.entity.DiscountCode;
import com.ziyara.backend.domain.enums.BookingStatus;
import com.ziyara.backend.domain.enums.NotificationType;
import com.ziyara.backend.domain.enums.ServiceType;
import com.ziyara.backend.domain.repository.BookingRepository;
import com.ziyara.backend.domain.repository.DiscountCodeRepository;
import com.ziyara.backend.domain.repository.ServiceRepository;
import com.ziyara.backend.domain.repository.UserRepository;
import com.ziyara.backend.infrastructure.messaging.StaffNotificationCommandPublisher;
import com.ziyara.backend.infrastructure.messaging.StaffNotificationEvent;
import com.ziyara.backend.modules.booking.api.BookingServiceApi;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Booking operations — the sole implementation of {@link BookingServiceApi}.
 * All domain repository access for booking-related use-cases lives here; no controller
 * imports a domain repository or entity directly.
 */
@Service
@RequiredArgsConstructor
public class BookingService implements BookingServiceApi {

    private final BookingRepository bookingRepository;
    private final ServiceRepository serviceRepository;
    private final UserRepository userRepository;
    private final DiscountCodeRepository discountCodeRepository;
    private final PricingEngineApi pricingService;
    private final TaxiBookingService taxiBookingService;
    // infrastructure.messaging — accepted cross-cutting dependency (see DddLayeringArchitectureTest)
    private final StaffNotificationCommandPublisher staffNotificationCommandPublisher;

    // ── Used by payment module ────────────────────────────────────────────────

    @Override
    @Transactional(readOnly = true)
    public Optional<BookingResponse> getBooking(UUID bookingId) {
        return bookingRepository.findById(bookingId).map(this::toResponse);
    }

    // ── Read ──────────────────────────────────────────────────────────────────

    @Override
    @Transactional(readOnly = true)
    public Page<BookingResponse> getAllBookings(UUID userId, boolean isCompanyStaff,
                                                BookingStatus status, boolean scopeAll,
                                                int page, int size) {
        PageRequest pr = bookingPageRequest(page, size);
        if (scopeAll) {
            Page<Booking> page_ = status != null
                    ? bookingRepository.findByStatus(status, pr)
                    : bookingRepository.findAll(pr);
            return page_.map(this::toResponse);
        }
        Page<Booking> page_ = status != null
                ? bookingRepository.findByCustomerIdAndStatus(userId, status, pr)
                : bookingRepository.findByCustomerId(userId, pr);
        return page_.map(this::toResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public BookingResponse getBookingById(UUID bookingId, UUID requestingUserId, boolean isCompanyStaff) {
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new ResourceNotFoundException("Booking not found"));
        assertCanAccess(booking, requestingUserId, isCompanyStaff);
        return toResponse(booking);
    }

    @Override
    @Transactional(readOnly = true)
    public BookingResponse getBookingByReference(String reference, UUID requestingUserId, boolean isCompanyStaff) {
        Booking booking = bookingRepository.findByBookingReference(reference)
                .orElseThrow(() -> new ResourceNotFoundException("Booking not found"));
        assertCanAccess(booking, requestingUserId, isCompanyStaff);
        return toResponse(booking);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<BookingResponse> listForCustomer(UUID customerId, @Nullable BookingStatus status,
                                                  int page, int size) {
        int p = Math.max(0, page);
        int s = Math.min(100, Math.max(1, size));
        PageRequest pr = PageRequest.of(p, s, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<Booking> bookings = status != null
                ? bookingRepository.findByCustomerIdAndStatus(customerId, status, pr)
                : bookingRepository.findByCustomerId(customerId, pr);
        return bookings.map(this::toResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<BookingResponse> listAllAdmin(@Nullable BookingStatus status,
                                               @Nullable UUID providerId,
                                               @Nullable ServiceType serviceType,
                                               @Nullable LocalDate from,
                                               @Nullable LocalDate to,
                                               int page, int size) {
        PageRequest pr = bookingPageRequest(page, size);
        if (providerId != null || serviceType != null) {
            List<com.ziyara.backend.domain.entity.Service> services = providerId != null
                    ? serviceRepository.findByProviderId(providerId)
                    : serviceRepository.findByType(serviceType);
            List<UUID> serviceIds = services.stream()
                    .map(com.ziyara.backend.domain.entity.Service::getId)
                    .collect(Collectors.toList());
            if (serviceIds.isEmpty()) {
                return Page.empty(pr);
            }
            return bookingRepository.findByServiceIdIn(serviceIds, pr).map(this::toResponse);
        }
        return bookingRepository.findFilteredAdmin(status, from, to, pr).map(this::toResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public VoucherResponse getVoucher(UUID bookingId, UUID requestingUserId, boolean isCompanyStaff) {
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new ResourceNotFoundException("Booking not found"));
        assertCanAccess(booking, requestingUserId, isCompanyStaff);
        com.ziyara.backend.domain.entity.Service service =
                serviceRepository.findById(booking.getServiceId()).orElse(null);
        com.ziyara.backend.domain.entity.User user =
                userRepository.findById(booking.getCustomerId()).orElse(null);
        return VoucherResponse.builder()
                .bookingId(booking.getId())
                .bookingReference(booking.getBookingReference())
                .checkInDate(booking.getCheckInDate())
                .checkOutDate(booking.getCheckOutDate())
                .serviceName(service != null ? service.getName() : null)
                .serviceId(booking.getServiceId())
                .customerEmail(user != null ? user.getEmail() : null)
                .customerId(booking.getCustomerId())
                .totalAmount(booking.getTotalAmount())
                .currency(booking.getCurrency())
                .build();
    }

    // ── Write ─────────────────────────────────────────────────────────────────

    @Override
    @Transactional
    public BookingResponse createBooking(UUID customerId, BookingRequest request) {
        serviceRepository.findById(request.getServiceId())
                .orElseThrow(() -> new ResourceNotFoundException("Service not found"));

        if (request.getCheckOutDate() != null) {
            boolean hasConflict = bookingRepository.hasConflictingBooking(
                    request.getServiceId(), request.getCheckInDate(), request.getCheckOutDate());
            if (hasConflict) {
                throw new BusinessException("Service is not available for the selected dates");
            }
        }

        PriceBreakdownResponse breakdown = pricingService.calculatePrice(
                PricePreviewRequest.builder()
                        .serviceId(request.getServiceId())
                        .checkInDate(request.getCheckInDate())
                        .checkOutDate(request.getCheckOutDate())
                        .guests(request.getGuests() != null ? request.getGuests() : 1)
                        .rooms(request.getRooms() != null ? request.getRooms() : 1)
                        .discountCode(request.getDiscountCode())
                        .menuItemIds(request.getMenuItemIds())
                        .menuSectionIds(request.getMenuSectionIds())
                        .roomTypeId(request.getRoomTypeId())
                        .build());

        BigDecimal totalDiscount =
                (breakdown.getProviderDiscountAmount() != null ? breakdown.getProviderDiscountAmount() : BigDecimal.ZERO)
                .add(breakdown.getCompanyDiscountAmount() != null ? breakdown.getCompanyDiscountAmount() : BigDecimal.ZERO);

        Booking booking = new Booking();
        booking.setCustomerId(customerId);
        booking.setServiceId(request.getServiceId());
        booking.setCheckInDate(request.getCheckInDate());
        booking.setCheckOutDate(request.getCheckOutDate());
        booking.setGuests(request.getGuests() != null ? request.getGuests() : 1);
        booking.setRooms(request.getRooms() != null ? request.getRooms() : 1);
        booking.setSpecialRequests(request.getSpecialRequests());
        booking.setIdDocumentUrl(request.getIdDocumentUrl());
        booking.setDiscountContextMenuItemIds(request.getMenuItemIds());
        booking.setDiscountContextMenuSectionIds(request.getMenuSectionIds());
        booking.setDiscountContextRoomTypeId(request.getRoomTypeId());
        booking.setCurrency(breakdown.getCurrency());
        booking.setBaseAmount(breakdown.getBaseAmount());
        booking.setDiscountAmount(totalDiscount);
        booking.setTaxAmount(breakdown.getTaxAmount());
        booking.setCommissionAmount(breakdown.getCommissionAmount());
        booking.setTotalAmount(breakdown.getTotalAmount());

        if (request.getDiscountCode() != null && !request.getDiscountCode().isBlank()) {
            discountCodeRepository.findByCode(request.getDiscountCode().trim().toUpperCase())
                    .filter(DiscountCode::isValid)
                    .ifPresent(dc -> {
                        booking.setDiscountCodeId(dc.getId());
                        dc.incrementUsage();
                        discountCodeRepository.save(dc);
                    });
        }

        booking.setBookingReference(generateBookingReference());
        return toResponse(bookingRepository.save(booking));
    }

    @Override
    @Transactional
    public BookingResponse updateBooking(UUID id, UpdateBookingRequest request,
                                          UUID requestingUserId, boolean isCompanyStaff) {
        Booking booking = bookingRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Booking not found"));
        assertCanAccess(booking, requestingUserId, isCompanyStaff);
        if (!booking.canBeModified()) {
            throw new BusinessException("Booking cannot be modified in its current status");
        }
        if (request.getCheckInDate() != null)    booking.setCheckInDate(request.getCheckInDate());
        if (request.getCheckOutDate() != null)   booking.setCheckOutDate(request.getCheckOutDate());
        if (request.getGuests() != null)         booking.setGuests(request.getGuests());
        if (request.getRooms() != null)          booking.setRooms(request.getRooms());
        if (request.getSpecialRequests() != null) booking.setSpecialRequests(request.getSpecialRequests());
        return toResponse(bookingRepository.save(booking));
    }

    @Override
    @Transactional
    public BookingResponse confirmBooking(UUID id, UUID requestingUserId, boolean isCompanyStaff) {
        Booking booking = bookingRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Booking not found"));
        assertCanAccess(booking, requestingUserId, isCompanyStaff);
        booking.confirm();
        Booking saved = bookingRepository.save(booking);
        staffNotificationCommandPublisher.publishAfterCommit(StaffNotificationEvent.builder()
                .eventId(UUID.randomUUID())
                .notificationType(NotificationType.BOOKING_CONFIRMED_STAFF.name())
                .title("Booking confirmed")
                .message("Booking " + saved.getBookingReference() + " was confirmed.")
                .notifyRoles(List.of("SALES_MANAGER", "SUPPORT_MANAGER"))
                .metadata("{\"bookingId\":\"" + saved.getId() + "\"}")
                .build());
        return toResponse(saved);
    }

    @Override
    @Transactional
    public BookingResponse rejectBooking(UUID id, UUID requestingUserId, String reason) {
        Booking booking = bookingRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Booking not found"));
        booking.reject(requestingUserId, reason);
        return toResponse(bookingRepository.save(booking));
    }

    @Override
    @Transactional
    public BookingResponse cancelBooking(UUID bookingId, UUID requestingUserId,
                                          boolean isCompanyStaff, String reason) {
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new ResourceNotFoundException("Booking not found"));
        assertCanAccess(booking, requestingUserId, isCompanyStaff);
        if (!booking.canBeCancelled()) {
            throw new BusinessException("Booking cannot be cancelled in its current status");
        }
        booking.cancel(requestingUserId, reason);
        return toResponse(bookingRepository.save(booking));
    }

    @Override
    @Transactional
    public TaxiBookingResponse addTaxi(UUID bookingId, AddTaxiRequest request,
                                        UUID requestingUserId, boolean isCompanyStaff) {
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new ResourceNotFoundException("Booking not found"));
        assertCanAccess(booking, requestingUserId, isCompanyStaff);
        return taxiBookingService.createForBooking(bookingId, request);
    }

    // ── Private helpers ───────────────────────────────────────────────────────

    private void assertCanAccess(Booking booking, UUID requestingUserId, boolean isCompanyStaff) {
        if (booking.getCustomerId().equals(requestingUserId) || isCompanyStaff) {
            return;
        }
        throw new UnauthorizedException("You don't have access to this booking");
    }

    private static PageRequest bookingPageRequest(int page, int size) {
        int p = Math.max(0, page);
        int s = Math.min(100, Math.max(1, size));
        return PageRequest.of(p, s, Sort.by(Sort.Direction.DESC, "createdAt"));
    }

    private static String generateBookingReference() {
        return "ZYB" + System.currentTimeMillis()
                + String.format("%04d", (int) (Math.random() * 10000));
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
                .rejectionReason(b.getRejectionReason())
                .delayReason(b.getDelayReason())
                .internalNotes(b.getInternalNotes())
                .createdAt(b.getCreatedAt())
                .canBeCancelled(b.canBeCancelled())
                .canBeModified(b.canBeModified())
                .build();
    }
}
