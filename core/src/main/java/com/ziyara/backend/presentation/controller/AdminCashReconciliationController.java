package com.ziyara.backend.presentation.controller;

import com.ziyara.backend.application.dto.ApiResponse;
import com.ziyara.backend.application.dto.request.ReconcileCashCollectionRequest;
import com.ziyara.backend.application.dto.response.CashCollectionResponse;
import com.ziyara.backend.application.service.CashReconciliationService;
import com.ziyara.backend.application.service.PaymentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

import static com.ziyara.backend.infrastructure.security.ApiAuthorizationExpressions.PAYMENTS_CASH_RECONCILE;

@RestController
@RequestMapping("/admin/cash")
@RequiredArgsConstructor
@PreAuthorize(PAYMENTS_CASH_RECONCILE)
@Tag(name = "Admin — Cash Reconciliation", description = "Finance reconciliation of provider cash collections")
@SecurityRequirement(name = "bearerAuth")
public class AdminCashReconciliationController {

    private final PaymentService paymentService;
    private final CashReconciliationService reconciliationService;

    @GetMapping("/pending-reconciliation")
    @Operation(summary = "List OPEN cash collections awaiting admin reconciliation")
    public ResponseEntity<Page<CashCollectionResponse>> pending(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(reconciliationService.pendingReconciliation(page, size));
    }

    @PostMapping("/collections/{id}/reconcile")
    @Operation(summary = "Mark a cash collection as reconciled (idempotent)")
    public ResponseEntity<ApiResponse<CashCollectionResponse>> reconcile(
            @PathVariable UUID id,
            @Valid @RequestBody(required = false) ReconcileCashCollectionRequest request) {
        UUID adminUserId = requireCurrentUserId();
        String notes = request != null ? request.getNotes() : null;
        CashCollectionResponse updated = paymentService.reconcileCashCollection(id, adminUserId, notes);
        return ResponseEntity.ok(ApiResponse.success("Cash reconciled", updated));
    }

    @PostMapping("/collections/{id}/dispute")
    @Operation(summary = "Mark a cash collection as disputed")
    public ResponseEntity<ApiResponse<CashCollectionResponse>> dispute(
            @PathVariable UUID id,
            @RequestBody(required = false) ReconcileCashCollectionRequest request) {
        UUID adminUserId = requireCurrentUserId();
        String reason = request != null ? request.getNotes() : null;
        CashCollectionResponse updated = reconciliationService.dispute(id, adminUserId, reason);
        return ResponseEntity.ok(ApiResponse.success("Cash disputed", updated));
    }

    private UUID requireCurrentUserId() {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getName() != null) {
            try {
                return UUID.fromString(auth.getName());
            } catch (IllegalArgumentException ignored) {
            }
        }
        throw new AccessDeniedException("Not authenticated");
    }
}
