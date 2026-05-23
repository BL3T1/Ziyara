package com.ziyara.backend.presentation.controller;

import com.ziyara.backend.application.dto.ApiResponse;
import com.ziyara.backend.application.dto.BookingResponse;
import com.ziyara.backend.application.dto.request.ApplyDiscountRequest;
import com.ziyara.backend.application.dto.request.ApplyDiscountToBookingRequest;
import com.ziyara.backend.application.dto.request.CreateDiscountRequest;
import com.ziyara.backend.application.dto.request.UpdateDiscountRequest;
import com.ziyara.backend.application.dto.response.DiscountResponse;
import com.ziyara.backend.application.query.DiscountQueryHandler;
import com.ziyara.backend.application.service.DiscountCodeService;
import com.ziyara.backend.domain.entity.DiscountCode;
import com.ziyara.backend.domain.enums.DiscountStatus;
import com.ziyara.backend.application.exception.ResourceNotFoundException;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import com.ziyara.backend.infrastructure.security.ApiAuthorizationExpressions;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Controller: DiscountController (Phase 2)
 * GET list/by-id = jOOQ; POST/PUT/DELETE/approve/deactivate = DiscountCodeService.
 */
@RestController
@RequestMapping("/discounts")
@RequiredArgsConstructor
@Tag(name = "Discounts", description = "Promotion and coupon APIs")
@SecurityRequirement(name = "bearerAuth")
public class DiscountController {

    private final DiscountCodeService discountService;
    private final DiscountQueryHandler discountQueryHandler;

    @GetMapping
    @PreAuthorize(ApiAuthorizationExpressions.COMPANY_STAFF)
    @Operation(summary = "List discounts", description = "Paginated list with optional status filter")
    public ResponseEntity<ApiResponse<Page<DiscountResponse>>> list(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) DiscountStatus status) {
        return ResponseEntity.ok(ApiResponse.success(discountQueryHandler.findPage(page, size, status)));
    }

    @GetMapping("/{id}")
    @PreAuthorize(ApiAuthorizationExpressions.COMPANY_STAFF)
    @Operation(summary = "Get discount", description = "Get discount by ID")
    public ResponseEntity<ApiResponse<DiscountResponse>> getById(@PathVariable UUID id) {
        return discountQueryHandler.findById(id)
                .map(r -> ResponseEntity.ok(ApiResponse.success(r)))
                .orElseThrow(() -> new ResourceNotFoundException("Discount not found"));
    }

    @PostMapping
    @PreAuthorize(ApiAuthorizationExpressions.DISCOUNT_CREATE)
    @Operation(
            summary = "Create discount",
            description = "Create a discount. Managers and sales submit as PENDING_APPROVAL until Super Admin or CEO approves; Super Admin and CEO may create as ACTIVE.")
    public ResponseEntity<ApiResponse<DiscountResponse>> create(@Valid @RequestBody CreateDiscountRequest request) {
        DiscountResponse response = discountService.create(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success("Discount created", response));
    }

    @PutMapping("/{id}")
    @PreAuthorize(ApiAuthorizationExpressions.COMPANY_STAFF)
    @Operation(summary = "Update discount", description = "Update discount code")
    public ResponseEntity<ApiResponse<DiscountResponse>> update(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateDiscountRequest request) {
        return ResponseEntity.ok(ApiResponse.success(discountService.update(id, request)));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize(ApiAuthorizationExpressions.COMPANY_STAFF)
    @Operation(summary = "Delete discount", description = "Delete discount code")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable UUID id) {
        discountService.deleteById(id);
        return ResponseEntity.ok(ApiResponse.success("Discount deleted", null));
    }

    @PostMapping("/{id}/approve")
    @PreAuthorize(ApiAuthorizationExpressions.DISCOUNT_APPROVE)
    @Operation(summary = "Approve discount", description = "Super Admin or CEO only: activate a pending discount")
    public ResponseEntity<ApiResponse<DiscountResponse>> approve(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.success(discountService.approve(id)));
    }

    @PostMapping("/{id}/deactivate")
    @PreAuthorize(ApiAuthorizationExpressions.COMPANY_STAFF)
    @Operation(summary = "Deactivate discount", description = "Set discount status to INACTIVE")
    public ResponseEntity<ApiResponse<DiscountResponse>> deactivate(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.success(discountService.deactivate(id)));
    }

    @PostMapping("/validate")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Validate code", description = "Check if a discount code is valid for a given amount")
    public ResponseEntity<ApiResponse<DiscountCode>> validateCode(
            @Valid @RequestBody ApplyDiscountRequest request,
            @RequestParam BigDecimal amount) {
        return discountService.validateCode(request, amount)
                .map(dc -> ResponseEntity.ok(ApiResponse.success("Code is valid", dc)))
                .orElseThrow(() -> new RuntimeException("Invalid or expired discount code"));
    }

    @PostMapping("/apply")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Apply discount to booking", description = "Apply a discount code to a booking; returns updated booking")
    public ResponseEntity<ApiResponse<BookingResponse>> apply(
            @Valid @RequestBody ApplyDiscountToBookingRequest request) {
        BookingResponse response = discountService.applyToBooking(request.getCode(), request.getBookingId());
        return ResponseEntity.ok(ApiResponse.success("Discount applied", response));
    }
}
