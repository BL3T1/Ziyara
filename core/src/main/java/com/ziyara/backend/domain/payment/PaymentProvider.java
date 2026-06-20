package com.ziyara.backend.domain.payment;

import com.ziyara.backend.domain.enums.PaymentStatus;

import java.util.Optional;

/**
 * Domain output port: payment gateway provider.
 * Implementations live in the infrastructure layer (Stripe, Flutterwave, stub).
 * PCI: the backend never receives raw card data; only a token from the gateway SDK.
 */
public interface PaymentProvider {

    /**
     * Initiate a charge with a tokenized card. Returns a redirect URL when 3DS is required.
     */
    GatewayChargeResult initiatePayment(GatewayChargeCommand command);

    /**
     * Refund a completed payment via the gateway.
     */
    GatewayRefundResult refund(GatewayRefundCommand command);

    /**
     * Optionally poll the current status from the gateway by its external reference.
     */
    Optional<PaymentStatus> getStatus(String gatewayReference);

    /** Gateway name for audit storage, e.g. "STRIPE", "FLUTTERWAVE". */
    String getProviderName();
}
