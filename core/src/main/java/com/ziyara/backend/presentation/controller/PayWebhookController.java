package com.ziyara.backend.presentation.controller;

import com.ziyara.backend.application.dto.ApiResponse;
import com.ziyara.backend.application.dto.response.PaymentResponse;
import com.ziyara.backend.modules.payment.api.PaymentServiceApi;
import com.ziyara.backend.infrastructure.payment.PaymentGatewayProperties;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * Public webhook endpoint for payment gateway callbacks (PAYMENT_METHODS, Phase 2).
 * Signature verified by WebhookSignatureFilter; invalid signature returns 403.
 * Processing is idempotent by gateway_reference.
 */
@RestController
@RequestMapping("/pay")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Pay Webhooks", description = "Gateway webhook callbacks (signature verification required)")
public class PayWebhookController {

    private final PaymentServiceApi paymentService;
    private final PaymentGatewayProperties gatewayProperties;

    @PostMapping("/webhooks")
    @Operation(summary = "Gateway webhook", description = "Receive success/failure from gateway. Verify X-Webhook-Signature (HMAC-SHA256 of body).")
    public ResponseEntity<ApiResponse<PaymentResponse>> webhook(@RequestBody Map<String, Object> payload) {
        log.info("Webhook received (keys): {}", payload != null ? payload.keySet() : "null");
        if (payload == null || payload.isEmpty()) {
            return ResponseEntity.badRequest().body(ApiResponse.error("Empty payload"));
        }
        // Common payload keys: transactionReference, gateway_reference, id, reference, status, type
        String gatewayRef = getString(payload, "gateway_reference", "transactionReference", "reference", "id", "transaction_ref");
        String status = getString(payload, "status", "type");
        String gatewayName = gatewayProperties.getProvider();

        if (gatewayRef != null && !gatewayRef.isBlank()) {
            if ("success".equalsIgnoreCase(status) || "completed".equalsIgnoreCase(status)
                    || "payment_intent.succeeded".equals(status) || "charge.succeeded".equals(status)) {
                return paymentService.completePaymentByGatewayReference(gatewayRef, gatewayName)
                        .map(r -> ResponseEntity.ok(ApiResponse.success("Payment completed", r)))
                        .orElse(ResponseEntity.ok(ApiResponse.success("Webhook received (no matching pending payment)", null)));
            }
            if ("failed".equalsIgnoreCase(status) || "error".equalsIgnoreCase(status)) {
                String errorMessage = getString(payload, "error_message", "message", "error");
                paymentService.failPaymentByGatewayReference(gatewayRef, errorMessage != null ? errorMessage : "Gateway reported failure");
            }
        }
        return ResponseEntity.ok(ApiResponse.success("Webhook received", (PaymentResponse) null));
    }

    private static String getString(Map<String, Object> payload, String... keys) {
        for (String key : keys) {
            Object v = payload.get(key);
            if (v != null && !v.toString().isBlank()) return v.toString();
            // snake_case variant
            String snake = key.replaceAll("([A-Z])", "_$1").toLowerCase().replace("__", "_");
            v = payload.get(snake);
            if (v != null && !v.toString().isBlank()) return v.toString();
        }
        return null;
    }
}

