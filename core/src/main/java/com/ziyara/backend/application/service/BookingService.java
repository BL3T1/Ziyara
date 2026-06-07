package com.ziyara.backend.application.service;

import com.ziyara.backend.application.dto.BookingRequest;
import com.ziyara.backend.application.dto.BookingResponse;
import com.ziyara.backend.application.dto.request.AddTaxiRequest;
import com.ziyara.backend.application.dto.request.PricePreviewRequest;
import com.ziyara.backend.application.dto.request.UpdateBookingRequest;
import com.ziyara.backend.application.dto.response.PriceBreakdownResponse;
import com.ziyara.backend.application.dto.response.TaxiBookingResponse;
import com.ziyara.backend.application.dto.response.VoucherResponse;
import com.ziyara.backend.application.annotation.Audited;
import com.ziyara.backend.application.exception.BusinessException;
import com.ziyara.backend.application.exception.ResourceNotFoundException;
import com.ziyara.backend.application.exception.UnauthorizedException;
import com.ziyara.backend.domain.usecase.booking.CancelBookingUseCase;
import com.ziyara.backend.domain.usecase.booking.ConfirmBookingUseCase;
import com.ziyara.backend.domain.usecase.booking.CreateBookingUseCase;
import com.ziyara.backend.modules.pricing.api.PricingEngineApi;
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
import com.ziyara.backend.modules.webhook.api.WebhookEventPublisher;
import lombok.RequiredArgsConstructor;
import com.ziyara.backend.domain.common.PageQuery;
import com.ziyara.backend.domain.common.PagedResult;
import com.ziyara.backend.infrastructure.persistence.util.PageConverter;
import org.springframework.data.domain.Page;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
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
    private final WebhookEventPublisher webhookEventPublisher;

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
        PageQuery query = bookingPageQuery(page, size);
        if (scopeAll) {
            PagedResult<Booking> result = status != null
                    ? bookingRepository.findByStatus(status, query)
                    : bookingRepository.findAll(query);
            return PageConverter.toSpringPage(result, query, this::toResponse);
        }
        PagedResult<Booking> result = status != null
                ? bookingRepository.findByCustomerIdAndStatus(userId, status, query)
                : bookingRepository.findByCustomerId(userId, query);
        return PageConverter.toSpringPage(result, query, this::toResponse);
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
        PageQuery query = bookingPageQuery(page, size);
        PagedResult<Booking> bookings = status != null
                ? bookingRepository.findByCustomerIdAndStatus(customerId, status, query)
                : bookingRepository.findByCustomerId(customerId, query);
        return PageConverter.toSpringPage(bookings, query, this::toResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<BookingResponse> listAllAdmin(@Nullable BookingStatus status,
                                               @Nullable UUID providerId,
                                               @Nullable ServiceType serviceType,
                                               @Nullable LocalDate from,
                                               @Nullable LocalDate to,
                                               int page, int size) {
        PageQuery query = bookingPageQuery(page, size);
        if (providerId != null || serviceType != null) {
            List<com.ziyara.backend.domain.entity.Service> services = providerId != null
                    ? serviceRepository.findByProviderId(providerId)
                    : serviceRepository.findByType(serviceType);
            List<UUID> serviceIds = services.stream()
                    .map(com.ziyara.backend.domain.entity.Service::getId)
                    .collect(Collectors.toList());
            if (serviceIds.isEmpty()) {
                return PageConverter.toSpringPage(PagedResult.empty(query), query);
            }
            return PageConverter.toSpringPage(bookingRepository.findByServiceIdIn(serviceIds, query), query, this::toResponse);
        }
        return PageConverter.toSpringPage(bookingRepository.findFilteredAdmin(status, from, to, query), query, this::toResponse);
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

    @Audited(action = "BOOKING_CREATE", entityType = "Booking")
    @Override
    @Transactional
    public BookingResponse createBooking(UUID customerId, BookingRequest request) {
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

        // Resolve discount code ID before delegating to use case
        DiscountCode resolvedDc = null;
        if (request.getDiscountCode() != null && !request.getDiscountCode().isBlank()) {
            resolvedDc = discountCodeRepository.findByCode(request.getDiscountCode().trim().toUpperCase())
                    .filter(DiscountCode::isValid).orElse(null);
        }

        var createResult = new CreateBookingUseCase(bookingRepository, serviceRepository).execute(
                new CreateBookingUseCase.Input(
                        customerId, request.getServiceId(),
                        request.getCheckInDate(), request.getCheckOutDate(),
                        request.getGuests() != null ? request.getGuests() : 1,
                        request.getRooms() != null ? request.getRooms() : 1,
                        breakdown.getBaseAmount(), totalDiscount,
                        breakdown.getTaxAmount(), breakdown.getTotalAmount(),
                        breakdown.getCurrency(), request.getSpecialRequests(),
                        resolvedDc != null ? resolvedDc.getId() : null));
        if (!createResult.success()) throw new BusinessException(createResult.error());

        // Enrich with application-level fields the use case doesn't manage
        Booking booking = createResult.booking();
        booking.setCommissionAmount(breakdown.getCommissionAmount());
        if (request.getIdDocumentUrl() != null) booking.setIdDocumentUrl(request.getIdDocumentUrl());
        if (request.getMenuItemIds() != null) booking.setDiscountContextMenuItemIds(request.getMenuItemIds());
        if (request.getMenuSectionIds() != null) booking.setDiscountContextMenuSectionIds(request.getMenuSectionIds());
        if (request.getRoomTypeId() != null) booking.setDiscountContextRoomTypeId(request.getRoomTypeId());
        if (request.getPaymentMethod() != null) booking.setPaymentMethod(request.getPaymentMethod());
        bookingRepository.save(booking);

        webhookEventPublisher.publishAfterCommit("booking.created", Map.of(
                "bookingId", booking.getId().toString(),
                "bookingReference", booking.getBookingReference(),
                "serviceId", booking.getServiceId().toString(),
                "customerId", booking.getCustomerId().toString(),
                "totalAmount", booking.getTotalAmount(),
                "currency", booking.getCurrency() != null ? booking.getCurrency() : "USD",
                "status", booking.getStatus().name()
        ));

        // Increment discount usage after successful booking creation
        if (resolvedDc != null) {
            resolvedDc.incrementUsage();
            discountCodeRepository.save(resolvedDc);
        }

        return toResponse(booking);
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

    @Audited(action = "BOOKING_CONFIRM", entityType = "Booking", entityIdArgIndex = 0)
    @Override
    @Transactional
    public BookingResponse confirmBooking(UUID id, UUID requestingUserId, boolean isCompanyStaff) {
        Booking existing = bookingRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Booking not found"));
        assertCanAccess(existing, requestingUserId, isCompanyStaff);
        var result = new ConfirmBookingUseCase(bookingRepository)
                .execute(new ConfirmBookingUseCase.Input(id, requestingUserId));
        if (!result.success()) throw new BusinessException(result.error());
        staffNotificationCommandPublisher.publishAfterCommit(StaffNotificationEvent.builder()
                .eventId(UUID.randomUUID())
                .notificationType(NotificationType.BOOKING_CONFIRMED_STAFF.name())
                .title("Booking confirmed")
                .message("Booking " + result.booking().getBookingReference() + " was confirmed.")
                .notifyRoles(List.of("SALES_MANAGER", "SALES_REPRESENTATIVE", "SUPPORT_MANAGER", "SUPPORT_AGENT"))
                .metadata("{\"bookingId\":\"" + result.booking().getId() + "\"}")
                .build());
        return toResponse(result.booking());
    }

    @Audited(action = "BOOKING_REJECT", entityType = "Booking", entityIdArgIndex = 0)
    @Override
    @Transactional
    public BookingResponse rejectBooking(UUID id, UUID requestingUserId, String reason) {
        Booking booking = bookingRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Booking not found"));
        booking.reject(requestingUserId, reason);
        return toResponse(bookingRepository.save(booking));
    }

    @Audited(action = "BOOKING_CANCEL", entityType = "Booking", entityIdArgIndex = 0)
    @Override
    @Transactional
    public BookingResponse cancelBooking(UUID bookingId, UUID requestingUserId,
                                          boolean isCompanyStaff, String reason) {
        Booking existing = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new ResourceNotFoundException("Booking not found"));
        assertCanAccess(existing, requestingUserId, isCompanyStaff);
        // Pass customerId only for customers (use case skips ownership check when null — staff path)
        UUID customerId = isCompanyStaff ? null : requestingUserId;
        var result = new CancelBookingUseCase(bookingRepository)
                .execute(new CancelBookingUseCase.Input(bookingId, customerId, requestingUserId, reason));
        if (!result.success()) throw new BusinessException(result.error());
        return toResponse(result.booking());
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

    private static PageQuery bookingPageQuery(int page, int size) {
        return PageQuery.of(Math.max(0, page), Math.min(100, Math.max(1, size)), "createdAt", false);
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
                .paymentMethod(b.getPaymentMethod())
                .paymentStatus(b.getPaymentStatus() != null ? b.getPaymentStatus().name() : null)
                .build();
    }
}
