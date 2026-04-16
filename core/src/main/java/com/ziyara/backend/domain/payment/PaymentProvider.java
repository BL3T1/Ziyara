package com.ziyara.backend.domain.payment;

import com.ziyara.backend.application.dto.payment.GatewayPaymentResponse;
import com.ziyara.backend.application.dto.payment.GatewayRefundResult;
import com.ziyara.backend.application.dto.payment.TokenizedPaymentCommand;
import com.ziyara.backend.domain.enums.PaymentStatus;

import java.math.BigDecimal;
import java.util.Optional;

/**
 * Port: Payment gateway provider (PAYMENT_METHODS, Phase 2).
 * Implementations: Stripe, Flutterwave, Visa Direct, or stub for sandbox.
 * PCI: Backend never receives raw card data; only token from gateway-hosted fields/SDK.
 */
public interface PaymentProvider {

    /**
     * Initiate payment with tokenized card. Returns redirect URL for 3DS when required.
     *
     * @param command tokenized request (no raw card data)
     * @return response with redirectUrl (3DS), transactionRef, status, threeDsStatus
     */
    GatewayPaymentResponse initiatePayment(TokenizedPaymentCommand command);

    /**
     * Refund a completed payment via the gateway.
     *
     * @param gatewayReference external transaction ID from gateway
     * @param amount            amount to refund
     * @param currency          currency code
     * @param reason            reason for refund (audit)
     * @return result with success and optional gateway refund reference
     */
    GatewayRefundResult refund(String gatewayReference, BigDecimal amount, String currency, String reason);

    /**
     * Optional: fetch current status from gateway by external reference.
     */
    Optional<PaymentStatus> getStatus(String gatewayReference);

    /** Provider name for storage (e.g. "STRIPE", "FLUTTERWAVE"). */
    String getProviderName();
}
