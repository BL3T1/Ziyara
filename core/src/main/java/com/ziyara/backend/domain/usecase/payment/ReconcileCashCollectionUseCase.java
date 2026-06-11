package com.ziyara.backend.domain.usecase.payment;

import com.ziyara.backend.domain.entity.CashCollection;
import com.ziyara.backend.domain.entity.Payment;
import com.ziyara.backend.domain.enums.CashCollectionStatus;
import com.ziyara.backend.domain.enums.PaymentStatus;
import com.ziyara.backend.domain.repository.CashCollectionRepository;
import com.ziyara.backend.domain.repository.PaymentRepository;

import java.util.Optional;
import java.util.UUID;

/**
 * Admin/finance action: mark a CashCollection as RECONCILED and bump the
 * linked Payment from COLLECTED → RECONCILED. Idempotent.
 */
public class ReconcileCashCollectionUseCase {

    private final CashCollectionRepository cashCollectionRepository;
    private final PaymentRepository paymentRepository;

    public ReconcileCashCollectionUseCase(CashCollectionRepository cashCollectionRepository,
                                          PaymentRepository paymentRepository) {
        this.cashCollectionRepository = cashCollectionRepository;
        this.paymentRepository = paymentRepository;
    }

    public Result execute(Input input) {
        if (input.collectionId() == null) return Result.failure("collectionId is required");
        if (input.adminUserId() == null) return Result.failure("adminUserId is required");

        Optional<CashCollection> opt = cashCollectionRepository.findById(input.collectionId());
        if (opt.isEmpty()) return Result.failure("Cash collection not found");

        CashCollection collection = opt.get();

        if (collection.getStatus() == CashCollectionStatus.RECONCILED) {
            return Result.success(collection, null, true);
        }
        if (collection.getStatus() == CashCollectionStatus.DISPUTED) {
            return Result.failure("Disputed collections cannot be reconciled. Resolve the dispute first.");
        }

        collection.reconcile(input.adminUserId());
        CashCollection savedCollection = cashCollectionRepository.save(collection);

        Payment updatedPayment = paymentRepository.findById(collection.getPaymentId())
                .map(p -> {
                    if (p.getStatus() == PaymentStatus.COLLECTED || p.getStatus() == PaymentStatus.RECORDED) {
                        p.setStatus(PaymentStatus.RECONCILED);
                        return paymentRepository.save(p);
                    }
                    return p;
                })
                .orElse(null);

        return Result.success(savedCollection, updatedPayment, false);
    }

    public record Input(UUID collectionId, UUID adminUserId, String notes) {}

    public record Result(boolean success, CashCollection collection, Payment payment, boolean wasIdempotent, String error) {
        public static Result success(CashCollection c, Payment p, boolean wasIdempotent) {
            return new Result(true, c, p, wasIdempotent, null);
        }
        public static Result failure(String error) {
            return new Result(false, null, null, false, error);
        }
    }
}
