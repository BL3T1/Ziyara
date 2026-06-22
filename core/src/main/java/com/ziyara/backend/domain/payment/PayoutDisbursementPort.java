package com.ziyara.backend.domain.payment;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Port for automated payout disbursement to provider bank accounts.
 * Implementations live in infrastructure (stub, real bank API, etc.).
 */
public interface PayoutDisbursementPort {

    /** Whether the implementation should auto-disburse on payout approval. */
    boolean isAutoDisburse();

    DisbursementResult disburse(DisbursementCommand command);

    record DisbursementCommand(
            UUID payoutId,
            UUID providerId,
            BigDecimal amount,
            String currency,
            String bankAccountRef,
            String bankAccountName
    ) {}

    record DisbursementResult(
            boolean success,
            String transactionRef,
            String error
    ) {}
}
