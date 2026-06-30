package com.ziyara.backend.presentation.controller;

import com.ziyara.backend.application.dto.ApiResponse;
import com.ziyara.backend.application.dto.request.AdminPayoutActionRequest;
import com.ziyara.backend.application.dto.request.BulkPayoutActionRequest;
import com.ziyara.backend.application.dto.request.CreateManualPayoutRequest;
import com.ziyara.backend.application.dto.response.AdminPayoutResponse;
import com.ziyara.backend.application.dto.response.AdminPayoutSummaryResponse;
import com.ziyara.backend.application.service.AdminPayoutService;
import com.ziyara.backend.infrastructure.security.SecurityContextUserId;
import static com.ziyara.backend.infrastructure.security.ApiAuthorizationExpressions.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;

import java.util.Map;
import java.util.UUID;

/**
 * Finance > Provider Payouts management.
 * All endpoints require payouts:read or payouts:write permission.
 */
@RestController
@RequestMapping("/admin/payouts")
@RequiredArgsConstructor
@Tag(name = "Admin Payouts", description = "Finance ops: review, approve, and disburse provider payouts")
@SecurityRequirement(name = "bearerAuth")
public class AdminPayoutController {

    private final AdminPayoutService adminPayoutService;

    @GetMapping("/summary")
    @PreAuthorize(PAYOUTS_READ)
    @Operation(summary = "Payout summary metrics", description = "6 KPI cards for the payouts page header")
    public ResponseEntity<ApiResponse<AdminPayoutSummaryResponse>> getSummary(
            @RequestParam(required = false) String start,
            @RequestParam(required = false) String end) {
        return ResponseEntity.ok(ApiResponse.success(adminPayoutService.getSummary(start, end)));
    }

    @GetMapping
    @PreAuthorize(PAYOUTS_READ)
    @Operation(summary = "List payout requests", description = "Paginated, filterable ledger of all provider payout requests")
    public ResponseEntity<ApiResponse<Page<AdminPayoutResponse>>> listPayouts(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String providerId,
            @RequestParam(required = false) String start,
            @RequestParam(required = false) String end,
            @RequestParam(required = false) String q) {
        return ResponseEntity.ok(ApiResponse.success(
                adminPayoutService.listPayouts(page, size, status, providerId, start, end, q)));
    }

    @GetMapping("/{id}")
    @PreAuthorize(PAYOUTS_READ)
    @Operation(summary = "Get payout detail", description = "Full detail including status history, for the detail drawer")
    public ResponseEntity<ApiResponse<AdminPayoutResponse>> getById(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.success(adminPayoutService.getById(id)));
    }

    @PostMapping("/{id}/approve")
    @PreAuthorize(PAYOUTS_WRITE)
    @Operation(summary = "Approve / initiate payment", description = "Transitions PENDING → PROCESSING")
    public ResponseEntity<ApiResponse<AdminPayoutResponse>> approve(
            @PathVariable UUID id,
            @RequestBody(required = false) AdminPayoutActionRequest req) {
        UUID actorId = SecurityContextUserId.currentUserId().orElse(null);
        return ResponseEntity.ok(ApiResponse.success("Payment initiated", adminPayoutService.approve(id, req, actorId)));
    }

    @PostMapping("/{id}/hold")
    @PreAuthorize(PAYOUTS_WRITE)
    @Operation(summary = "Put on hold", description = "Transitions PENDING → ON_HOLD")
    public ResponseEntity<ApiResponse<AdminPayoutResponse>> hold(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.success("Payout placed on hold", adminPayoutService.hold(id)));
    }

    @PostMapping("/{id}/release-hold")
    @PreAuthorize(PAYOUTS_WRITE)
    @Operation(summary = "Release hold", description = "Transitions ON_HOLD → PENDING")
    public ResponseEntity<ApiResponse<AdminPayoutResponse>> releaseHold(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.success("Hold released", adminPayoutService.releaseHold(id)));
    }

    @PostMapping("/{id}/cancel")
    @PreAuthorize(PAYOUTS_WRITE)
    @Operation(summary = "Cancel payout", description = "Terminal transition to CANCELLED")
    public ResponseEntity<ApiResponse<AdminPayoutResponse>> cancel(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.success("Payout cancelled", adminPayoutService.cancel(id)));
    }

    @PostMapping("/{id}/retry")
    @PreAuthorize(PAYOUTS_WRITE)
    @Operation(summary = "Retry failed payment", description = "Transitions FAILED/REJECTED → PENDING")
    public ResponseEntity<ApiResponse<AdminPayoutResponse>> retry(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.success("Payout queued for retry", adminPayoutService.retry(id)));
    }

    @PostMapping("/{id}/mark-paid")
    @PreAuthorize(PAYOUTS_APPROVE)
    @Operation(summary = "Mark as manually paid", description = "Force-completes a payout with an external transaction reference")
    public ResponseEntity<ApiResponse<AdminPayoutResponse>> markPaid(
            @PathVariable UUID id,
            @RequestBody(required = false) AdminPayoutActionRequest req) {
        UUID actorId = SecurityContextUserId.currentUserId().orElse(null);
        return ResponseEntity.ok(ApiResponse.success("Marked as paid", adminPayoutService.markPaid(id, req, actorId)));
    }

    @PostMapping("/{id}/schedule")
    @PreAuthorize(PAYOUTS_WRITE)
    @Operation(summary = "Schedule payout", description = "Transitions PENDING → SCHEDULED with a future date")
    public ResponseEntity<ApiResponse<AdminPayoutResponse>> schedule(
            @PathVariable UUID id,
            @RequestBody AdminPayoutActionRequest req) {
        return ResponseEntity.ok(ApiResponse.success("Payout scheduled", adminPayoutService.schedule(id, req)));
    }

    @PatchMapping("/{id}/notes")
    @PreAuthorize(PAYOUTS_WRITE)
    @Operation(summary = "Update internal notes")
    public ResponseEntity<ApiResponse<AdminPayoutResponse>> updateNotes(
            @PathVariable UUID id,
            @RequestBody Map<String, String> body) {
        return ResponseEntity.ok(ApiResponse.success(adminPayoutService.updateNotes(id, body.get("notes"))));
    }

    @PostMapping("/bulk/approve")
    @PreAuthorize(PAYOUTS_APPROVE)
    @Operation(summary = "Bulk approve/pay", description = "Initiates payment for multiple PENDING payouts")
    public ResponseEntity<ApiResponse<Map<String, Object>>> bulkApprove(
            @Valid @RequestBody BulkPayoutActionRequest req) {
        UUID actorId = SecurityContextUserId.currentUserId().orElse(null);
        return ResponseEntity.ok(ApiResponse.success(adminPayoutService.bulkApprove(req, actorId)));
    }

    @PostMapping("/bulk/hold")
    @PreAuthorize(PAYOUTS_WRITE)
    @Operation(summary = "Bulk hold")
    public ResponseEntity<ApiResponse<Map<String, Object>>> bulkHold(
            @Valid @RequestBody BulkPayoutActionRequest req) {
        return ResponseEntity.ok(ApiResponse.success(adminPayoutService.bulkHold(req)));
    }

    @PostMapping("/bulk/release-hold")
    @PreAuthorize(PAYOUTS_WRITE)
    @Operation(summary = "Bulk release hold")
    public ResponseEntity<ApiResponse<Map<String, Object>>> bulkReleaseHold(
            @Valid @RequestBody BulkPayoutActionRequest req) {
        return ResponseEntity.ok(ApiResponse.success(adminPayoutService.bulkReleaseHold(req)));
    }

    @GetMapping("/export")
    @PreAuthorize(PAYOUTS_READ)
    @Operation(summary = "Export payouts as CSV")
    public ResponseEntity<byte[]> exportCsv(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String start,
            @RequestParam(required = false) String end) {
        byte[] csv = adminPayoutService.exportCsv(status, start, end);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"payouts.csv\"")
                .contentType(MediaType.parseMediaType("text/csv"))
                .body(csv);
    }

    @PostMapping("/manual")
    @PreAuthorize(PAYOUTS_WRITE)
    @Operation(summary = "Create manual payout", description = "Off-cycle payment that enters the ledger immediately")
    public ResponseEntity<ApiResponse<AdminPayoutResponse>> createManual(
            @Valid @RequestBody CreateManualPayoutRequest req) {
        UUID actorId = SecurityContextUserId.currentUserId().orElse(null);
        return ResponseEntity.ok(ApiResponse.success("Manual payout created", adminPayoutService.createManual(req, actorId)));
    }
}
