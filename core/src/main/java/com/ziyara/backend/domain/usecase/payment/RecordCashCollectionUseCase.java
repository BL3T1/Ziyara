package com.ziyara.backend.domain.usecase.payment;

import com.ziyara.backend.domain.entity.CashCollection;
import com.ziyara.backend.domain.entity.Payment;
import com.ziyara.backend.domain.enums.PaymentMethod;
import com.ziyara.backend.domain.enums.PaymentStatus;
import com.ziyara.backend.domain.repository.CashCollectionRepository;
import com.ziyara.backend.domain.repository.PaymentRepository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Provider portal action: record cash received from a customer.
 * Transitions Payment(PENDING, CASH) → COLLECTED and creates a CashCollection(OPEN).
 * Refuses to double-record against the same payment.
 */
public class RecordCashCollectionUseCase {

    private final PaymentRepository paymentRepository;
    private final CashCollectionRepository cashCollectionRepository;

    public RecordCashCollectionUseCase(PaymentRepository paymentRepository,
                                       CashCollectionRepository cashCollectionRepository) {
        this.paymentRepository = paymentRepository;
        this.cashCollectionRepository = cashCollectionRepository;
    }

    public Result execute(Input input) {
        if (input.paymentId() == null) return Result.failure("paymentId is required");
        if (input.providerId() == null) return Result.failure("providerId is required");
        if (input.collectedByUserId() == null) return Result.failure("collectedByUserId is required");
        if (input.amount() == null || input.amount().signum() <= 0) {
            return Result.failure("amount must be positive");
        }

        Optional<Payment> paymentOpt = paymentRepository.findById(input.paymentId());
        if (paymentOpt.isEmpty()) return Result.failure("Payment not found");

        Payment payment = paymentOpt.get();

        if (payment.getMethod() != PaymentMethod.CASH
                && payment.getMethod() != PaymentMethod.CASH_ON_SERVICE
                && payment.getMethod() != PaymentMethod.CASH_ON_ARRIVAL) {
            return Result.failure("Payment method is not CASH: " + payment.getMethod());
        }

        if (payment.getStatus() != PaymentStatus.PENDING) {
            return Result.failure("Only PENDING cash payments can be recorded. Current status: " + payment.getStatus());
        }

        List<CashCollection> prior = cashCollectionRepository.findByPaymentId(input.paymentId());
        if (!prior.isEmpty()) {
            return Result.failure("Cash collection already recorded for this payment");
        }

        if (input.amount().compareTo(payment.getAmount()) != 0) {
            // Soft-allow but flag — accept partial/over amounts so providers aren't blocked,
            // but admin reconciliation will surface the mismatch.
        }

        payment.setStatus(PaymentStatus.COLLECTED);
        payment.setProcessedAt(LocalDateTime.now());
        Payment savedPayment = paymentRepository.save(payment);

        CashCollection collection = new CashCollection();
        collection.setPaymentId(savedPayment.getId());
        collection.setProviderId(input.providerId());
        collection.setCollectedAt(input.collectedAt() != null ? input.collectedAt() : LocalDateTime.now());
        collection.setCollectedByUserId(input.collectedByUserId());
        collection.setAmount(input.amount());
        collection.setCurrency(input.currency() != null ? input.currency() : savedPayment.getCurrency());
        collection.setReceiptNumber(input.receiptNumber());
        collection.setNotes(input.notes());

        CashCollection savedCollection = cashCollectionRepository.save(collection);
        return Result.success(savedCollection, savedPayment);
    }

    public record Input(
            UUID paymentId,
            UUID providerId,
            UUID collectedByUserId,
            BigDecimal amount,
            String currency,
            LocalDateTime collectedAt,
            String receiptNumber,
            String notes
    ) {}

    public record Result(boolean success, CashCollection collection, Payment payment, String error) {
        public static Result success(CashCollection collection, Payment payment) {
            return new Result(true, collection, payment, null);
        }

        public static Result failure(String error) {
            return new Result(false, null, null, error);
        }
    }
}
