package com.ziyara.backend.domain.payment;

import java.math.BigDecimal;

/**
 * Domain-level command for initiating a gateway refund.
 * Pure Java record — no framework dependency.
 */
public record GatewayRefundCommand(
        String gatewayReference,
        BigDecimal amount,
        String currency,
        String reason
) {}
