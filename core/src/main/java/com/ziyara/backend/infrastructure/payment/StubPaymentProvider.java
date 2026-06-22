package com.ziyara.backend.infrastructure.payment;

import com.ziyara.backend.domain.enums.PaymentStatus;
import com.ziyara.backend.domain.payment.GatewayChargeCommand;
import com.ziyara.backend.domain.payment.GatewayChargeResult;
import com.ziyara.backend.domain.payment.GatewayRefundCommand;
import com.ziyara.backend.domain.payment.GatewayRefundResult;
import com.ziyara.backend.domain.payment.PaymentProvider;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.UUID;

/**
 * Stub payment provider for sandbox/testing.
 * No real HTTP calls — returns success with fake references.
 */
@Component
@ConditionalOnProperty(name = "app.payment.gateway.provider", havingValue = "stub")
@Slf4j
public class StubPaymentProvider implements PaymentProvider {

    private static final String STUB_PREFIX = "stub_";

    @Override
    public GatewayChargeResult initiatePayment(GatewayChargeCommand command) {
        log.debug("Stub gateway: initiate payment {} amount {}", command.paymentId(), command.amount());
        String ref = command.idempotencyKey() != null && !command.idempotencyKey().isBlank()
                ? STUB_PREFIX + command.idempotencyKey()
                : STUB_PREFIX + UUID.randomUUID();
        return GatewayChargeResult.success(command.paymentId(), ref, ref, "NOT_REQUIRED", null);
    }

    @Override
    public GatewayRefundResult refund(GatewayRefundCommand command) {
        log.debug("Stub gateway: refund {} {} for ref {}", command.amount(), command.currency(), command.gatewayReference());
        return GatewayRefundResult.success(STUB_PREFIX + "refund_" + UUID.randomUUID());
    }

    @Override
    public Optional<PaymentStatus> getStatus(String gatewayReference) {
        return Optional.of(PaymentStatus.COMPLETED);
    }

    @Override
    public String getProviderName() {
        return "STUB";
    }
}
