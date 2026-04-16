package com.ziyara.backend.presentation.controller;

import com.ziyara.backend.application.dto.ApiResponse;
import com.ziyara.backend.application.dto.request.ConfirmPaymentRequest;
import com.ziyara.backend.application.dto.request.CreatePaymentRequest;
import com.ziyara.backend.application.dto.request.RefundRequest;
import com.ziyara.backend.application.dto.response.PaymentResponse;
import com.ziyara.backend.application.dto.response.RefundResponse;
import com.ziyara.backend.domain.enums.PaymentStatus;
import com.ziyara.backend.modules.payment.api.PaymentServiceApi;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.data.domain.Page;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

/**
 * Controller: PaymentController
 * Handles payment processing and history
 */
@RestController
@RequestMapping("/payments")
@RequiredArgsConstructor
@Tag(name = "Payments", description = "Payment processing APIs")
@SecurityRequirement(name = "bearerAuth")
public class PaymentController {
    
    private final PaymentServiceApi paymentService;
    
    @PostMapping
    @Operation(summary = "Process payment", description = "Initiate payment (idempotent when idempotencyKey is provided). For card, use token from gateway SDK.")
    public ResponseEntity<ApiResponse<PaymentResponse>> processPayment(
            @Valid @RequestBody CreatePaymentRequest request
    ) {
        PaymentResponse response = paymentService.initiatePayment(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Payment initiated", response));
    }
    
    @PostMapping("/initiate")
    @Operation(summary = "Initiate payment", description = "Start a new payment transaction (same as POST /payments with idempotency support)")
    public ResponseEntity<ApiResponse<PaymentResponse>> initiatePayment(
            @Valid @RequestBody CreatePaymentRequest request
    ) {
        PaymentResponse response = paymentService.initiatePayment(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Payment initiated", response));
    }
    
    @PostMapping("/{id}/complete")
    @Operation(summary = "Complete payment", description = "Confirm a successful payment via gateway callback (query params)")
    public ResponseEntity<ApiResponse<PaymentResponse>> completePayment(
            @PathVariable UUID id,
            @RequestParam String reference,
            @RequestParam String gateway
    ) {
        PaymentResponse response = paymentService.completePayment(id, reference, gateway);
        return ResponseEntity.ok(ApiResponse.success("Payment completed successfully", response));
    }

    @PostMapping("/{id}/confirm")
    @Operation(summary = "Confirm payment after 3DS", description = "Confirm payment after 3DS redirect; sets gateway reference and 3DS status.")
    public ResponseEntity<ApiResponse<PaymentResponse>> confirmPayment(
            @PathVariable UUID id,
            @Valid @RequestBody ConfirmPaymentRequest request
    ) {
        PaymentResponse response = paymentService.completePayment(
                id,
                request.getReference(),
                request.getGateway(),
                request.getGatewayReference(),
                request.getThreeDsStatus());
        return ResponseEntity.ok(ApiResponse.success("Payment confirmed", response));
    }
    
    @PostMapping("/{id}/fail")
    @Operation(summary = "Fail payment", description = "Record a failed payment attempt")
    public ResponseEntity<ApiResponse<PaymentResponse>> failPayment(
            @PathVariable UUID id,
            @RequestParam String reason
    ) {
        PaymentResponse response = paymentService.failPayment(id, reason);
        return ResponseEntity.ok(ApiResponse.success("Payment failed recorded", response));
    }

    @PostMapping("/{id}/refund")
    @Operation(summary = "Refund payment", description = "Create a refund for a completed payment. Reason is mandatory for audit.")
    public ResponseEntity<ApiResponse<RefundResponse>> refund(
            @PathVariable UUID id,
            @Valid @RequestBody RefundRequest request
    ) {
        UUID currentUserId = getCurrentUserId();
        RefundResponse response = paymentService.refund(id, request, currentUserId);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success("Refund created", response));
    }

    private UUID getCurrentUserId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.isAuthenticated() && auth.getName() != null && !"anonymousUser".equals(auth.getName())) {
            try {
                return UUID.fromString(auth.getName());
            } catch (IllegalArgumentException ignored) { }
        }
        return null;
    }

    @GetMapping
    @Operation(summary = "List payments (paged)", description = "Transaction ledger for dashboard (Financials); optional status filter")
    public ResponseEntity<ApiResponse<Page<PaymentResponse>>> listPayments(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) PaymentStatus status
    ) {
        return ResponseEntity.ok(ApiResponse.success(paymentService.getPayments(page, size, status)));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get payment", description = "Get payment details by ID")
    public ResponseEntity<ApiResponse<PaymentResponse>> getPayment(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.success(paymentService.getPayment(id)));
    }

    @GetMapping("/transaction/{ref}")
    @Operation(summary = "Get by transaction ref", description = "Get payment by gateway transaction reference")
    public ResponseEntity<ApiResponse<PaymentResponse>> getByTransactionRef(@PathVariable String ref) {
        return ResponseEntity.ok(ApiResponse.success(paymentService.getByTransactionRef(ref)));
    }
}
