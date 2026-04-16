package com.ziyara.backend.application.dto.payment;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Tokenized payment request for gateway (no raw card data – PCI safe).
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TokenizedPaymentCommand {

    /** Our internal payment ID (PENDING record created first). */
    private UUID paymentId;
    private UUID bookingId;
    private BigDecimal amount;
    private String currency;
    /** Token from gateway SDK (e.g. Stripe token, Flutterwave tx_ref). */
    private String paymentToken;
    /** Client idempotency key. */
    private String idempotencyKey;
}
