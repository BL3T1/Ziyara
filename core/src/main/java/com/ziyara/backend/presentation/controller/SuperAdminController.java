package com.ziyara.backend.presentation.controller;

import com.ziyara.backend.application.dto.ApiResponse;
import com.ziyara.backend.application.dto.BookingResponse;
import com.ziyara.backend.application.dto.UserResponse;
import com.ziyara.backend.application.dto.request.RestoreDeletedRequest;
import com.ziyara.backend.application.dto.response.DeletedItemResponse;
import com.ziyara.backend.application.dto.response.PaymentResponse;
import com.ziyara.backend.application.service.BookingService;
import com.ziyara.backend.application.service.PaymentService;
import com.ziyara.backend.application.service.SuperAdminRecoveryService;
import com.ziyara.backend.domain.enums.BookingStatus;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/**
 * Super-admin tools: customer lookup and soft-delete recycle bin (users & services).
 */
@RestController
@RequestMapping("/admin/super")
@RequiredArgsConstructor
@Tag(name = "Super admin", description = "Super-admin only recovery and lookup")
@SecurityRequirement(name = "bearerAuth")
public class SuperAdminController {

    private final SuperAdminRecoveryService superAdminRecoveryService;
    private final BookingService bookingService;
    private final PaymentService paymentService;

    @GetMapping("/customers/search")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    @Operation(summary = "Search customers", description = "Find non-deleted users with role CUSTOMER by email, phone, name (customers profile), or exact user id")
    public ResponseEntity<ApiResponse<List<UserResponse>>> searchCustomers(
            @RequestParam String q,
            @RequestParam(defaultValue = "25") int limit
    ) {
        return ResponseEntity.ok(ApiResponse.success(superAdminRecoveryService.searchCustomers(q, limit)));
    }

    @GetMapping("/deleted/recent")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    @Operation(summary = "Recent deleted", description = "Latest soft-deleted users and services by deleted_at (no search term)")
    public ResponseEntity<ApiResponse<List<DeletedItemResponse>>> listRecentDeleted(
            @RequestParam(defaultValue = "50") int limit
    ) {
        return ResponseEntity.ok(ApiResponse.success(superAdminRecoveryService.listRecentDeleted(limit)));
    }

    @GetMapping("/deleted/search")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    @Operation(summary = "Search deleted", description = "Soft-deleted users (email, phone) and services (name); UUID search is not supported")
    public ResponseEntity<ApiResponse<List<DeletedItemResponse>>> searchDeleted(
            @RequestParam String q,
            @RequestParam(defaultValue = "50") int limit
    ) {
        return ResponseEntity.ok(ApiResponse.success(superAdminRecoveryService.searchDeleted(q, limit)));
    }

    @GetMapping("/customers/{userId}/bookings")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    @Operation(summary = "Customer bookings (super admin)", description = "Paged bookings for an active customer user")
    public ResponseEntity<ApiResponse<Page<BookingResponse>>> customerBookings(
            @PathVariable UUID userId,
            @RequestParam(required = false) BookingStatus status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        superAdminRecoveryService.assertActiveCustomer(userId);
        return ResponseEntity.ok(ApiResponse.success(bookingService.listForCustomer(userId, status, page, size)));
    }

    @GetMapping("/customers/{userId}/payments")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    @Operation(summary = "Customer payments (super admin)", description = "Paged payments linked to bookings owned by this customer user")
    public ResponseEntity<ApiResponse<Page<PaymentResponse>>> customerPayments(
            @PathVariable UUID userId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        superAdminRecoveryService.assertActiveCustomer(userId);
        return ResponseEntity.ok(ApiResponse.success(paymentService.pageForCustomerUserId(userId, page, size)));
    }

    @PostMapping("/deleted/restore")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    @Operation(summary = "Restore deleted", description = "Clear deleted_at for USER (sys_users) or SERVICE (hotel_services)")
    public ResponseEntity<ApiResponse<Void>> restore(@Valid @RequestBody RestoreDeletedRequest request) {
        superAdminRecoveryService.restore(request.getEntityType(), request.getId());
        return ResponseEntity.ok(ApiResponse.success("Restored", null));
    }
}
