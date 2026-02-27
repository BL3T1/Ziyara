package com.ziyarah.presentation.controller;

import com.ziyarah.application.dto.ApiResponse;
import com.ziyarah.application.dto.request.CreatePaymentRequest;
import com.ziyarah.application.dto.response.PaymentResponse;
import com.ziyarah.application.service.PaymentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
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
    
    private final PaymentService paymentService;
    
    @PostMapping("/initiate")
    @Operation(summary = "Initiate payment", description = "Start a new payment transaction")
    public ResponseEntity<ApiResponse<PaymentResponse>> initiatePayment(
            @Valid @RequestBody CreatePaymentRequest request
    ) {
        PaymentResponse response = paymentService.initiatePayment(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Payment initiated", response));
    }
    
    @PostMapping("/{id}/complete")
    @Operation(summary = "Complete payment", description = "Confirm a successful payment via gateway callback")
    public ResponseEntity<ApiResponse<PaymentResponse>> completePayment(
            @PathVariable UUID id,
            @RequestParam String reference,
            @RequestParam String gateway
    ) {
        PaymentResponse response = paymentService.completePayment(id, reference, gateway);
        return ResponseEntity.ok(ApiResponse.success("Payment completed successfully", response));
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
}
