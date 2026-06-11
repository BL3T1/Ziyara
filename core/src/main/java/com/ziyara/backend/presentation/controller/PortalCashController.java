package com.ziyara.backend.presentation.controller;

import com.ziyara.backend.application.dto.ApiResponse;
import com.ziyara.backend.application.dto.request.RecordCashCollectionRequest;
import com.ziyara.backend.application.dto.response.CashCollectionResponse;
import com.ziyara.backend.application.service.CashReconciliationService;
import com.ziyara.backend.application.service.PaymentService;
import com.ziyara.backend.application.service.ServiceProviderService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static com.ziyara.backend.infrastructure.security.ApiAuthorizationExpressions.PORTAL_FINANCE;
import static com.ziyara.backend.infrastructure.security.ApiAuthorizationExpressions.PROVIDER_PORTAL;

@RestController
@RequestMapping("/portal/cash")
@RequiredArgsConstructor
@PreAuthorize(PROVIDER_PORTAL)
@Tag(name = "Provider Portal — Cash", description = "Record and review cash collections")
@SecurityRequirement(name = "bearerAuth")
public class PortalCashController {

    private final PaymentService paymentService;
    private final CashReconciliationService reconciliationService;
    private final ServiceProviderService providerService;

    @PostMapping("/bookings/{bookingId}/record")
    @PreAuthorize(PORTAL_FINANCE)
    @Operation(summary = "Record cash collected from a customer against a booking")
    public ResponseEntity<ApiResponse<CashCollectionResponse>> record(
            @PathVariable UUID bookingId,
            @Valid @RequestBody RecordCashCollectionRequest request) {
        UUID providerId = requireCurrentProviderId();
        UUID userId = requireCurrentUserId();
        CashCollectionResponse created = paymentService.recordCashCollectionForBooking(
                bookingId, providerId, userId, request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Cash recorded", created));
    }

    @GetMapping("/collections")
    @PreAuthorize(PORTAL_FINANCE)
    @Operation(summary = "List OPEN cash collections for the current provider")
    public ResponseEntity<ApiResponse<List<CashCollectionResponse>>> openCollections() {
        UUID providerId = requireCurrentProviderId();
        return ResponseEntity.ok(ApiResponse.success(paymentService.listOpenCashForProvider(providerId)));
    }

    @GetMapping("/daily-sheet")
    @PreAuthorize(PORTAL_FINANCE)
    @Operation(summary = "Daily cash sheet for the current provider on the given date")
    public ResponseEntity<ApiResponse<List<CashCollectionResponse>>> dailySheet(
            @RequestParam(name = "date", required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        UUID providerId = requireCurrentProviderId();
        LocalDate target = date != null ? date : LocalDate.now();
        return ResponseEntity.ok(ApiResponse.success(reconciliationService.dailyCashSheet(providerId, target)));
    }

    private UUID requireCurrentProviderId() {
        UUID userId = requireCurrentUserId();
        return providerService.getProviderByUserId(userId)
                .map(p -> p.getId())
                .orElseThrow(() -> new AccessDeniedException("No provider profile for this user"));
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
