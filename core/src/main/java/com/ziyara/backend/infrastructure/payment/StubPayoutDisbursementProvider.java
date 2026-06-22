package com.ziyara.backend.infrastructure.payment;

import com.ziyara.backend.domain.payment.PayoutDisbursementPort;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * No-op disbursement provider used in dev/test and as the default until a real integration is configured.
 * Activated when gateway.disbursement.provider=stub (or property is absent).
 */
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "gateway.disbursement.provider", havingValue = "stub", matchIfMissing = true)
public class StubPayoutDisbursementProvider implements PayoutDisbursementPort {

    private static final Logger log = LoggerFactory.getLogger(StubPayoutDisbursementProvider.class);

    private final PayoutDisbursementProperties properties;

    @Override
    public boolean isAutoDisburse() {
        return properties.isAutoDisburse();
    }

    @Override
    public DisbursementResult disburse(DisbursementCommand command) {
        String ref = "STUB-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        log.info("Stub disbursement — payout={} provider={} amount={} {} → ref={}",
                command.payoutId(), command.providerId(),
                command.amount(), command.currency(), ref);
        return new DisbursementResult(true, ref, null);
    }
}
