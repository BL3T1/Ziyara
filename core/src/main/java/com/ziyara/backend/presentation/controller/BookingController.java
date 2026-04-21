package com.ziyara.backend.presentation.controller;

import com.ziyara.backend.application.dto.ApiResponse;
import com.ziyara.backend.application.dto.BookingRequest;
import com.ziyara.backend.application.dto.BookingResponse;
import com.ziyara.backend.application.dto.request.AddTaxiRequest;
import com.ziyara.backend.application.dto.request.PricePreviewRequest;
import com.ziyara.backend.application.dto.request.UpdateBookingRequest;
import com.ziyara.backend.application.dto.response.PriceBreakdownResponse;
import com.ziyara.backend.application.dto.response.TaxiBookingResponse;
import com.ziyara.backend.application.dto.response.VoucherResponse;
import com.ziyara.backend.core.api.PricingEngineApi;
import com.ziyara.backend.application.service.TaxiBookingService;
import com.ziyara.backend.domain.entity.Booking;
import com.ziyara.backend.domain.entity.DiscountCode;
import com.ziyara.backend.domain.repository.BookingRepository;
import com.ziyara.backend.domain.repository.DiscountCodeRepository;
import com.ziyara.backend.domain.repository.ServiceRepository;
import com.ziyara.backend.domain.repository.UserRepository;
import com.ziyara.backend.infrastructure.security.JwtService;
import com.ziyara.backend.presentation.exception.BusinessException;
import com.ziyara.backend.presentation.exception.ResourceNotFoundException;
import com.ziyara.backend.presentation.exception.UnauthorizedException;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import com.ziyara.backend.domain.enums.BookingStatus;
import com.ziyara.backend.domain.enums.NotificationType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import com.ziyara.backend.infrastructure.security.ApiAuthorizationExpressions;
import com.ziyara.backend.infrastructure.messaging.StaffNotificationCommandPublisher;
import com.ziyara.backend.infrastructure.messaging.StaffNotificationEvent;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Controller: BookingController
 * Handles booking endpoints
 */
@RestController
@RequestMapping("/bookings")
@RequiredArgsConstructor
@Tag(name = "Bookings", description = "Booking management APIs")
@SecurityRequirement(name = "bearerAuth")
public class BookingController {
    
    private final BookingRepository bookingRepository;
    private final ServiceRepository serviceRepository;
    private final UserRepository userRepository;
    private final PricingEngineApi pricingService;
    private final DiscountCodeRepository discountCodeRepository;
    private final TaxiBookingService taxiBookingService;
    private final JwtService jwtService;
    private final StaffNotificationCommandPublisher staffNotificationCommandPublisher;
    
    @GetMapping
    @Operation(summary = "Get bookings (paged)", description = "Customer: own bookings. Company staff: ?scope=all for all bookings. Optional status filter.")
    public ResponseEntity<ApiResponse<Page<BookingResponse>>> getAllBookings(
            @RequestHeader("Authorization") String authHeader,
            @RequestParam(required = false) String scope,
            @RequestParam(required = false) BookingStatus status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        if ("all".equalsIgnoreCase(scope)
                && !ApiAuthorizationExpressions.isCompanyStaff(SecurityContextHolder.getContext().getAuthentication())) {
            throw new AccessDeniedException("Company staff only for scope=all");
        }
        UUID userId = extractUserId(authHeader);
        PageRequest pr = bookingPageRequest(page, size);
        Page<Booking> bookings;
        if ("all".equalsIgnoreCase(scope)) {
            bookings = status != null
                    ? bookingRepository.findByStatus(status, pr)
                    : bookingRepository.findAll(pr);
        } else {
            bookings = status != null
                    ? bookingRepository.findByCustomerIdAndStatus(userId, status, pr)
                    : bookingRepository.findByCustomerId(userId, pr);
        }
        Page<BookingResponse> responses = bookings.map(this::toBookingResponse);
        return ResponseEntity.ok(ApiResponse.success(responses));
    }

    @GetMapping("/admin")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'GENERAL_MANAGER', 'SALES_MANAGER', 'FINANCE_MANAGER', 'SUPPORT_MANAGER')")
    @Operation(summary = "List all bookings (admin, paged)", description = "Company dashboard: all bookings with optional status filter")
    public ResponseEntity<ApiResponse<Page<BookingResponse>>> listAllBookings(
            @RequestParam(required = false) BookingStatus status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        PageRequest pr = bookingPageRequest(page, size);
        Page<Booking> bookings = status != null
                ? bookingRepository.findByStatus(status, pr)
                : bookingRepository.findAll(pr);
        return ResponseEntity.ok(ApiResponse.success(bookings.map(this::toBookingResponse)));
    }
    
    @GetMapping("/{id}")
    @Operation(summary = "Get booking by ID", description = "Customer or company staff")
    public ResponseEntity<ApiResponse<BookingResponse>> getBookingById(
            @PathVariable UUID id,
            @RequestHeader("Authorization") String authHeader
    ) {
        UUID userId = extractUserId(authHeader);
        Booking booking = bookingRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Booking not found"));
        assertCanAccessBooking(booking, userId);
        return ResponseEntity.ok(ApiResponse.success(toBookingResponse(booking)));
    }
    
    @GetMapping("/reference/{reference}")
    @Operation(summary = "Get booking by reference", description = "Customer or company staff")
    public ResponseEntity<ApiResponse<BookingResponse>> getBookingByReference(
            @PathVariable String reference,
            @RequestHeader("Authorization") String authHeader
    ) {
        UUID userId = extractUserId(authHeader);
        Booking booking = bookingRepository.findByBookingReference(reference)
                .orElseThrow(() -> new ResourceNotFoundException("Booking not found"));
        assertCanAccessBooking(booking, userId);
        return ResponseEntity.ok(ApiResponse.success(toBookingResponse(booking)));
    }
    
    @PostMapping
    @Operation(summary = "Create booking", description = "Create a new booking")
    public ResponseEntity<ApiResponse<BookingResponse>> createBooking(
            @Valid @RequestBody BookingRequest request,
            @RequestHeader("Authorization") String authHeader
    ) {
        UUID userId = extractUserId(authHeader);
        
        // Validate service exists
        serviceRepository.findById(request.getServiceId())
                .orElseThrow(() -> new ResourceNotFoundException("Service not found"));
        
        // Check availability
        if (request.getCheckOutDate() != null) {
            boolean hasConflict = bookingRepository.hasConflictingBooking(
                    request.getServiceId(),
                    request.getCheckInDate(),
                    request.getCheckOutDate()
            );
            if (hasConflict) {
                throw new BusinessException("Service is not available for the selected dates");
            }
        }
        
        // Price breakdown (PRICING_METHODS: stacked discounts, commission; discount scope via menu/room context)
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
        java.math.BigDecimal totalDiscount = (breakdown.getProviderDiscountAmount() != null ? breakdown.getProviderDiscountAmount() : java.math.BigDecimal.ZERO)
                .add(breakdown.getCompanyDiscountAmount() != null ? breakdown.getCompanyDiscountAmount() : java.math.BigDecimal.ZERO);

        // Create booking
        Booking booking = new Booking();
        booking.setCustomerId(userId);
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
        Booking savedBooking = bookingRepository.save(booking);
        
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Booking created successfully", toBookingResponse(savedBooking)));
    }
    
    @PutMapping("/{id}")
    @Operation(summary = "Update booking", description = "Update booking when modifiable (Phase 3)")
    public ResponseEntity<ApiResponse<BookingResponse>> updateBooking(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateBookingRequest request,
            @RequestHeader("Authorization") String authHeader) {
        UUID userId = extractUserId(authHeader);
        Booking booking = bookingRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Booking not found"));
        assertCanAccessBooking(booking, userId);
        if (!booking.canBeModified()) {
            throw new BusinessException("Booking cannot be modified in its current status");
        }
        if (request.getCheckInDate() != null) booking.setCheckInDate(request.getCheckInDate());
        if (request.getCheckOutDate() != null) booking.setCheckOutDate(request.getCheckOutDate());
        if (request.getGuests() != null) booking.setGuests(request.getGuests());
        if (request.getRooms() != null) booking.setRooms(request.getRooms());
        if (request.getSpecialRequests() != null) booking.setSpecialRequests(request.getSpecialRequests());
        Booking saved = bookingRepository.save(booking);
        return ResponseEntity.ok(ApiResponse.success(toBookingResponse(saved)));
    }

    @PostMapping("/{id}/confirm")
    @Operation(summary = "Confirm booking", description = "Customer or company staff")
    public ResponseEntity<ApiResponse<BookingResponse>> confirmBooking(
            @PathVariable UUID id,
            @RequestHeader("Authorization") String authHeader) {
        UUID userId = extractUserId(authHeader);
        Booking booking = bookingRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Booking not found"));
        assertCanAccessBooking(booking, userId);
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
        return ResponseEntity.ok(ApiResponse.success(toBookingResponse(saved)));
    }

    @PostMapping("/{id}/taxi")
    @Operation(summary = "Add taxi", description = "Add taxi add-on to booking (Phase 3)")
    public ResponseEntity<ApiResponse<TaxiBookingResponse>> addTaxi(
            @PathVariable UUID id,
            @Valid @RequestBody AddTaxiRequest request,
            @RequestHeader("Authorization") String authHeader) {
        UUID userId = extractUserId(authHeader);
        Booking booking = bookingRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Booking not found"));
        assertCanAccessBooking(booking, userId);
        TaxiBookingResponse response = taxiBookingService.createForBooking(id, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success("Taxi add-on created", response));
    }

    @GetMapping("/{id}/voucher")
    @Operation(summary = "Get voucher", description = "Get booking voucher (Phase 3); customer or company staff")
    public ResponseEntity<ApiResponse<VoucherResponse>> getVoucher(
            @PathVariable UUID id,
            @RequestHeader("Authorization") String authHeader) {
        UUID userId = extractUserId(authHeader);
        Booking booking = bookingRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Booking not found"));
        assertCanAccessBooking(booking, userId);
        com.ziyara.backend.domain.entity.Service service = serviceRepository.findById(booking.getServiceId()).orElse(null);
        com.ziyara.backend.domain.entity.User user = userRepository.findById(booking.getCustomerId()).orElse(null);
        VoucherResponse voucher = VoucherResponse.builder()
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
        return ResponseEntity.ok(ApiResponse.success(voucher));
    }

    @PostMapping("/{id}/cancel")
    @Operation(summary = "Cancel booking", description = "Cancel an existing booking; customer or company staff")
    public ResponseEntity<ApiResponse<BookingResponse>> cancelBooking(
            @PathVariable UUID id,
            @RequestParam(required = false) String reason,
            @RequestHeader("Authorization") String authHeader
    ) {
        UUID userId = extractUserId(authHeader);
        Booking booking = bookingRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Booking not found"));
        assertCanAccessBooking(booking, userId);
        if (!booking.canBeCancelled()) {
            throw new BusinessException("Booking cannot be cancelled in its current status");
        }
        booking.cancel(userId, reason);
        Booking savedBooking = bookingRepository.save(booking);
        return ResponseEntity.ok(ApiResponse.success("Booking cancelled successfully", toBookingResponse(savedBooking)));
    }
    
    private static PageRequest bookingPageRequest(int page, int size) {
        int p = Math.max(0, page);
        int s = Math.min(100, Math.max(1, size));
        return PageRequest.of(p, s, Sort.by(Sort.Direction.DESC, "createdAt"));
    }

    private void assertCanAccessBooking(Booking booking, UUID tokenUserId) {
        if (booking.getCustomerId().equals(tokenUserId)) {
            return;
        }
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (ApiAuthorizationExpressions.isCompanyStaff(auth)) {
            return;
        }
        throw new UnauthorizedException("You don't have access to this booking");
    }
    
    private UUID extractUserId(String authHeader) {
        String token = authHeader.substring(7);
        return UUID.fromString(jwtService.extractUserId(token));
    }
    
    private String generateBookingReference() {
        return "ZYB" + System.currentTimeMillis() + String.format("%04d", (int)(Math.random() * 10000));
    }
    
    private BookingResponse toBookingResponse(Booking booking) {
        return BookingResponse.builder()
                .id(booking.getId())
                .bookingReference(booking.getBookingReference())
                .customerId(booking.getCustomerId())
                .serviceId(booking.getServiceId())
                .checkInDate(booking.getCheckInDate())
                .checkOutDate(booking.getCheckOutDate())
                .guests(booking.getGuests())
                .rooms(booking.getRooms())
                .baseAmount(booking.getBaseAmount())
                .discountAmount(booking.getDiscountAmount())
                .taxAmount(booking.getTaxAmount())
                .commissionAmount(booking.getCommissionAmount())
                .totalAmount(booking.getTotalAmount())
                .currency(booking.getCurrency())
                .status(booking.getStatus())
                .specialRequests(booking.getSpecialRequests())
                .idDocumentVerified(booking.isIdDocumentVerified())
                .confirmedAt(booking.getConfirmedAt())
                .cancelledAt(booking.getCancelledAt())
                .cancellationReason(booking.getCancellationReason())
                .createdAt(booking.getCreatedAt())
                .canBeCancelled(booking.canBeCancelled())
                .canBeModified(booking.canBeModified())
                .build();
    }
    
}
