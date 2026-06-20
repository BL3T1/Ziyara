package com.ziyara.backend.domain.payment;

/**
 * Domain-level result of a gateway refund attempt.
 * Pure Java record — no framework dependency.
 */
public record GatewayRefundResult(
        boolean success,
        String gatewayRefundId,
        String errorMessage
) {
    public static GatewayRefundResult success(String gatewayRefundId) {
        return new GatewayRefundResult(true, gatewayRefundId, null);
    }

    public static GatewayRefundResult failure(String errorMessage) {
        return new GatewayRefundResult(false, null, errorMessage);
    }
}
