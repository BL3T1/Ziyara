package com.ziyara.backend.domain.payment;

import com.ziyara.backend.domain.enums.PaymentStatus;

import java.util.UUID;

/**
 * Domain-level result of a gateway charge attempt.
 * Pure Java record — no framework dependency.
 */
public record GatewayChargeResult(
        boolean success,
        UUID paymentId,
        String transactionReference,
        String gatewayReference,
        PaymentStatus status,
        String threeDsStatus,
        String redirectUrl,
        String errorMessage
) {
    public static GatewayChargeResult success(UUID paymentId, String transactionReference,
                                               String gatewayReference, String threeDsStatus,
                                               String redirectUrl) {
        return new GatewayChargeResult(true, paymentId, transactionReference,
                gatewayReference, PaymentStatus.COMPLETED, threeDsStatus, redirectUrl, null);
    }

    public static GatewayChargeResult failure(UUID paymentId, String errorMessage) {
        return new GatewayChargeResult(false, paymentId, null, null,
                PaymentStatus.FAILED, null, null, errorMessage);
    }
}
