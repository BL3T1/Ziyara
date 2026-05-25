package com.ziyara.backend.domain.payment;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Domain-level command for initiating a gateway charge.
 * Pure Java record — no framework dependency.
 * The infrastructure adapter maps this to gateway-specific request objects.
 */
public record GatewayChargeCommand(
        UUID paymentId,
        UUID bookingId,
        BigDecimal amount,
        String currency,
        String paymentToken,
        String idempotencyKey
) {}
