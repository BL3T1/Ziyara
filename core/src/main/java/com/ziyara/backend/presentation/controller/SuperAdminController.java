package com.ziyara.backend.presentation.controller;

import com.ziyara.backend.application.dto.ApiResponse;
import com.ziyara.backend.application.dto.BookingResponse;
import com.ziyara.backend.application.dto.UserResponse;
import com.ziyara.backend.application.dto.request.RestoreDeletedRequest;
import com.ziyara.backend.application.dto.response.DeletedItemResponse;
import com.ziyara.backend.application.dto.response.PaymentResponse;
import com.ziyara.backend.modules.booking.api.BookingServiceApi;
import com.ziyara.backend.modules.payment.api.PaymentServiceApi;
import com.ziyara.backend.application.service.SuperAdminRecoveryService;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import static com.ziyara.backend.infrastructure.security.ApiAuthorizationExpressions.CUSTOMERS_READ;
import static com.ziyara.backend.infrastructure.security.ApiAuthorizationExpressions.DELETED_ITEMS_READ;
import static com.ziyara.backend.infrastructure.security.ApiAuthorizationExpressions.DELETED_ITEMS_RESTORE;
import static com.ziyara.backend.infrastructure.security.ApiAuthorizationExpressions.BOOKINGS_READ;
import static com.ziyara.backend.infrastructure.security.ApiAuthorizationExpressions.PAYMENTS_READ;
import java.util.HashSet;
import java.util.Set;
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
    private final BookingServiceApi bookingService;
    private final PaymentServiceApi paymentService;

    @GetMapping("/customers/search")
    @PreAuthorize(CUSTOMERS_READ)
    @Operation(summary = "Search customers", description = "Find non-deleted users with role CUSTOMER by email, phone, name (customers profile), or exact user id")
    public ResponseEntity<ApiResponse<List<UserResponse>>> searchCustomers(
            @RequestParam String q,
            @RequestParam(defaultValue = "25") int limit
    ) {
        return ResponseEntity.ok(ApiResponse.success(superAdminRecoveryService.searchCustomers(q, limit)));
    }

    @GetMapping("/deleted/recent")
    @PreAuthorize(DELETED_ITEMS_READ)
    @Operation(summary = "Recent deleted", description = "Latest soft-deleted items. Results filtered by caller's per-tab permissions.")
    public ResponseEntity<ApiResponse<List<DeletedItemResponse>>> listRecentDeleted(
            @RequestParam(defaultValue = "50") int limit,
            Authentication auth
    ) {
        return ResponseEntity.ok(ApiResponse.success(superAdminRecoveryService.listRecentDeleted(limit, resolveAllowedEntityTypes(auth))));
    }

    @GetMapping("/deleted/search")
    @PreAuthorize(DELETED_ITEMS_READ)
    @Operation(summary = "Search deleted", description = "Soft-deleted items filtered by caller's per-tab permissions; UUID search not supported.")
    public ResponseEntity<ApiResponse<List<DeletedItemResponse>>> searchDeleted(
            @RequestParam String q,
            @RequestParam(defaultValue = "50") int limit,
            Authentication auth
    ) {
        return ResponseEntity.ok(ApiResponse.success(superAdminRecoveryService.searchDeleted(q, limit, resolveAllowedEntityTypes(auth))));
    }

    /** Derive which entity-type categories the caller may see based on their granted authorities. */
    private static Set<String> resolveAllowedEntityTypes(Authentication auth) {
        if (auth == null) return Set.of();
        Set<String> allowed = new HashSet<>();
        boolean hasAll = false;
        for (GrantedAuthority ga : auth.getAuthorities()) {
            String a = ga.getAuthority();
            if ("deleted_items:read".equals(a)) { hasAll = true; break; }
            if ("deleted_items:company:read".equals(a))   allowed.add("USER_COMPANY");
            if ("deleted_items:providers:read".equals(a)) { allowed.add("SERVICE"); allowed.add("PROVIDER"); }
            if ("deleted_items:users:read".equals(a))     allowed.add("USER_CUSTOMER");
        }
        if (hasAll) return Set.of("USER_COMPANY", "USER_CUSTOMER", "SERVICE", "PROVIDER");
        return allowed;
    }

    @GetMapping("/customers/{userId}/bookings")
    @PreAuthorize(CUSTOMERS_READ + " or " + BOOKINGS_READ)
    @Operation(summary = "Customer bookings", description = "Paged bookings for an active customer user")
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
    @PreAuthorize(CUSTOMERS_READ + " or " + PAYMENTS_READ)
    @Operation(summary = "Customer payments", description = "Paged payments linked to bookings owned by this customer user")
    public ResponseEntity<ApiResponse<Page<PaymentResponse>>> customerPayments(
            @PathVariable UUID userId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        superAdminRecoveryService.assertActiveCustomer(userId);
        return ResponseEntity.ok(ApiResponse.success(paymentService.pageForCustomerUserId(userId, page, size)));
    }

    @PostMapping("/deleted/restore")
    @PreAuthorize(DELETED_ITEMS_RESTORE)
    @Operation(summary = "Restore deleted", description = "Clear deleted_at for USER (sys_users) or SERVICE (hotel_services)")
    public ResponseEntity<ApiResponse<Void>> restore(@Valid @RequestBody RestoreDeletedRequest request) {
        superAdminRecoveryService.restore(request.getEntityType(), request.getId());
        return ResponseEntity.ok(ApiResponse.success("Restored", null));
    }

    @DeleteMapping("/deleted/permanent")
    @PreAuthorize(DELETED_ITEMS_RESTORE)
    @Operation(summary = "Permanently delete", description = "Hard-delete a soft-deleted USER or SERVICE row. Only works on rows where deleted_at IS NOT NULL.")
    public ResponseEntity<ApiResponse<Void>> permanentDelete(@Valid @RequestBody RestoreDeletedRequest request) {
        superAdminRecoveryService.permanentDelete(request.getEntityType(), request.getId());
        return ResponseEntity.ok(ApiResponse.success("Permanently deleted", null));
    }
}
