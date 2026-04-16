package com.ziyara.backend.application.dto.payment;

import com.ziyara.backend.domain.enums.PaymentStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

/**
 * Response from payment gateway: redirect for 3DS, transaction ref, status.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GatewayPaymentResponse {

    private boolean success;
    private UUID paymentId;
    /** External transaction ID from gateway (for idempotency and reconciliation). */
    private String transactionReference;
    /** Gateway reference / charge ID (same or separate per gateway). */
    private String gatewayReference;
    private PaymentStatus status;
    /** e.g. AUTHENTICATED, NOT_REQUIRED, FAILED. */
    private String threeDsStatus;
    /** When 3DS required: URL to redirect the user. */
    private String redirectUrl;
    private String errorMessage;
}
