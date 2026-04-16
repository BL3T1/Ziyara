package com.ziyara.backend.infrastructure.payment;

import com.ziyara.backend.application.dto.payment.GatewayPaymentResponse;
import com.ziyara.backend.application.dto.payment.GatewayRefundResult;
import com.ziyara.backend.application.dto.payment.TokenizedPaymentCommand;
import com.ziyara.backend.domain.enums.PaymentStatus;
import com.ziyara.backend.domain.payment.PaymentProvider;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

/**
 * Stub payment provider for sandbox/testing (PAYMENT_METHODS, Phase 2).
 * No real HTTP calls; returns success with fake references. Can simulate 3DS redirect.
 */
@Component
@ConditionalOnProperty(name = "app.payment.gateway.provider", havingValue = "stub")
@Slf4j
public class StubPaymentProvider implements PaymentProvider {

    private static final String STUB_PREFIX = "stub_";

    @Override
    public GatewayPaymentResponse initiatePayment(TokenizedPaymentCommand command) {
        log.debug("Stub gateway: initiate payment for {} amount {}", command.getPaymentId(), command.getAmount());
        String ref = command.getIdempotencyKey() != null && !command.getIdempotencyKey().isBlank()
                ? STUB_PREFIX + command.getIdempotencyKey()
                : STUB_PREFIX + UUID.randomUUID();
        return GatewayPaymentResponse.builder()
                .success(true)
                .paymentId(command.getPaymentId())
                .transactionReference(ref)
                .gatewayReference(ref)
                .status(PaymentStatus.COMPLETED)
                .threeDsStatus("NOT_REQUIRED")
                .redirectUrl(null)
                .build();
    }

    @Override
    public GatewayRefundResult refund(String gatewayReference, BigDecimal amount, String currency, String reason) {
        log.debug("Stub gateway: refund {} {} for ref {}", amount, currency, gatewayReference);
        return GatewayRefundResult.builder()
                .success(true)
                .gatewayRefundId(STUB_PREFIX + "refund_" + UUID.randomUUID())
                .build();
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
