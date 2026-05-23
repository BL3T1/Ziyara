package com.ziyara.backend.presentation.controller;

import com.ziyara.backend.application.dto.ApiResponse;
import com.ziyara.backend.application.dto.BookingRequest;
import com.ziyara.backend.application.dto.BookingResponse;
import com.ziyara.backend.application.dto.request.AddTaxiRequest;
import com.ziyara.backend.application.dto.request.UpdateBookingRequest;
import com.ziyara.backend.application.dto.response.TaxiBookingResponse;
import com.ziyara.backend.application.dto.response.VoucherResponse;
import com.ziyara.backend.domain.enums.BookingStatus;
import com.ziyara.backend.domain.enums.ServiceType;
import com.ziyara.backend.infrastructure.security.ApiAuthorizationExpressions;
import com.ziyara.backend.infrastructure.security.JwtService;
import com.ziyara.backend.modules.booking.api.BookingServiceApi;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.UUID;

/**
 * Booking HTTP surface — all business logic is delegated to {@link BookingServiceApi}.
 * No domain repository or entity is imported here.
 */
@RestController
@RequestMapping("/bookings")
@RequiredArgsConstructor
@Tag(name = "Bookings", description = "Booking management APIs")
@SecurityRequirement(name = "bearerAuth")
public class BookingController {

    private final BookingServiceApi bookingService;
    private final JwtService jwtService;

    @GetMapping
    @Operation(summary = "Get bookings (paged)",
               description = "Customer: own bookings. Company staff: ?scope=all for all bookings. Optional status filter.")
    public ResponseEntity<ApiResponse<Page<BookingResponse>>> getAllBookings(
            @RequestHeader("Authorization") String authHeader,
            @RequestParam(required = false) String scope,
            @RequestParam(required = false) BookingStatus status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        boolean isStaff = isCompanyStaff();
        if ("all".equalsIgnoreCase(scope) && !isStaff) {
            throw new AccessDeniedException("Company staff only for scope=all");
        }
        return ResponseEntity.ok(ApiResponse.success(
                bookingService.getAllBookings(
                        extractUserId(authHeader), isStaff, status,
                        "all".equalsIgnoreCase(scope), page, size)));
    }

    @GetMapping("/my")
    @Operation(summary = "My bookings", description = "Alias for GET /bookings — always returns only the authenticated customer's own bookings")
    public ResponseEntity<ApiResponse<Page<BookingResponse>>> getMyBookings(
            @RequestHeader("Authorization") String authHeader,
            @RequestParam(required = false) BookingStatus status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(ApiResponse.success(
                bookingService.getAllBookings(extractUserId(authHeader), false, status, false, page, size)));
    }

    @GetMapping("/admin")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'GENERAL_MANAGER', 'SALES_MANAGER', 'FINANCE_MANAGER', 'SUPPORT_MANAGER')")
    @Operation(summary = "List all bookings (admin, paged)",
               description = "Company dashboard: all bookings with optional filters")
    public ResponseEntity<ApiResponse<Page<BookingResponse>>> listAllBookings(
            @RequestParam(required = false) BookingStatus status,
            @RequestParam(required = false) UUID providerId,
            @RequestParam(required = false) ServiceType serviceType,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateFrom,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateTo,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(ApiResponse.success(
                bookingService.listAllAdmin(status, providerId, serviceType, dateFrom, dateTo, page, size)));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get booking by ID", description = "Customer or company staff")
    public ResponseEntity<ApiResponse<BookingResponse>> getBookingById(
            @PathVariable UUID id,
            @RequestHeader("Authorization") String authHeader) {
        return ResponseEntity.ok(ApiResponse.success(
                bookingService.getBookingById(id, extractUserId(authHeader), isCompanyStaff())));
    }

    @GetMapping("/reference/{reference}")
    @Operation(summary = "Get booking by reference", description = "Customer or company staff")
    public ResponseEntity<ApiResponse<BookingResponse>> getBookingByReference(
            @PathVariable String reference,
            @RequestHeader("Authorization") String authHeader) {
        return ResponseEntity.ok(ApiResponse.success(
                bookingService.getBookingByReference(reference, extractUserId(authHeader), isCompanyStaff())));
    }

    @PostMapping
    @Operation(summary = "Create booking", description = "Create a new booking")
    public ResponseEntity<ApiResponse<BookingResponse>> createBooking(
            @Valid @RequestBody BookingRequest request,
            @RequestHeader("Authorization") String authHeader) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Booking created successfully",
                        bookingService.createBooking(extractUserId(authHeader), request)));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update booking", description = "Update booking when modifiable")
    public ResponseEntity<ApiResponse<BookingResponse>> updateBooking(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateBookingRequest request,
            @RequestHeader("Authorization") String authHeader) {
        return ResponseEntity.ok(ApiResponse.success(
                bookingService.updateBooking(id, request, extractUserId(authHeader), isCompanyStaff())));
    }

    @PostMapping("/{id}/confirm")
    @Operation(summary = "Confirm booking", description = "Customer or company staff")
    public ResponseEntity<ApiResponse<BookingResponse>> confirmBooking(
            @PathVariable UUID id,
            @RequestHeader("Authorization") String authHeader) {
        return ResponseEntity.ok(ApiResponse.success(
                bookingService.confirmBooking(id, extractUserId(authHeader), isCompanyStaff())));
    }

    @PostMapping("/{id}/taxi")
    @Operation(summary = "Add taxi", description = "Add taxi add-on to booking")
    public ResponseEntity<ApiResponse<TaxiBookingResponse>> addTaxi(
            @PathVariable UUID id,
            @Valid @RequestBody AddTaxiRequest request,
            @RequestHeader("Authorization") String authHeader) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Taxi add-on created",
                        bookingService.addTaxi(id, request, extractUserId(authHeader), isCompanyStaff())));
    }

    @GetMapping("/{id}/voucher")
    @Operation(summary = "Get voucher", description = "Get booking voucher; customer or company staff")
    public ResponseEntity<ApiResponse<VoucherResponse>> getVoucher(
            @PathVariable UUID id,
            @RequestHeader("Authorization") String authHeader) {
        return ResponseEntity.ok(ApiResponse.success(
                bookingService.getVoucher(id, extractUserId(authHeader), isCompanyStaff())));
    }

    @PostMapping("/{id}/reject")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'GENERAL_MANAGER', 'SALES_MANAGER', 'SUPPORT_MANAGER')")
    @Operation(summary = "Reject booking", description = "Admin: reject a pending booking with a reason")
    public ResponseEntity<ApiResponse<BookingResponse>> rejectBooking(
            @PathVariable UUID id,
            @RequestParam(required = false) String reason,
            @RequestHeader("Authorization") String authHeader) {
        return ResponseEntity.ok(ApiResponse.success("Booking rejected",
                bookingService.rejectBooking(id, extractUserId(authHeader), reason)));
    }

    @PostMapping("/{id}/cancel")
    @Operation(summary = "Cancel booking", description = "Cancel an existing booking; customer or company staff")
    public ResponseEntity<ApiResponse<BookingResponse>> cancelBooking(
            @PathVariable UUID id,
            @RequestParam(required = false) String reason,
            @RequestHeader("Authorization") String authHeader) {
        return ResponseEntity.ok(ApiResponse.success("Booking cancelled successfully",
                bookingService.cancelBooking(id, extractUserId(authHeader), isCompanyStaff(), reason)));
    }

    // ── Private helpers ───────────────────────────────────────────────────────

    private UUID extractUserId(String authHeader) {
        return UUID.fromString(jwtService.extractUserId(authHeader.substring(7)));
    }

    private static boolean isCompanyStaff() {
        return ApiAuthorizationExpressions.isCompanyStaff(
                SecurityContextHolder.getContext().getAuthentication());
    }
}
