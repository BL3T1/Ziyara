package com.ziyara.backend.domain.usecase.refund;

import com.ziyara.backend.domain.entity.Refund;
import com.ziyara.backend.domain.enums.RefundStatus;
import com.ziyara.backend.domain.repository.RefundRepository;

import java.util.Optional;
import java.util.UUID;

public class ProcessRefundUseCase {

    private final RefundRepository refundRepository;

    public ProcessRefundUseCase(RefundRepository refundRepository) {
        this.refundRepository = refundRepository;
    }

    public Result execute(Input input) {
        Optional<Refund> refundOpt = refundRepository.findById(input.refundId());
        if (refundOpt.isEmpty()) {
            return Result.failure("Refund not found");
        }

        Refund refund = refundOpt.get();

        if (refund.getStatus() != RefundStatus.REQUESTED && refund.getStatus() != RefundStatus.PENDING) {
            return Result.failure("Refund cannot be processed. Current status: " + refund.getStatus());
        }

        refund.setStatus(RefundStatus.PROCESSED);
        refund.setTransactionReference(input.transactionReference());
        refund.setProcessedBy(input.processedBy());
        refund.setProcessedAt(java.time.LocalDateTime.now());

        Refund saved = refundRepository.save(refund);
        return Result.success(saved);
    }

    public record Input(UUID refundId, String transactionReference, UUID processedBy) {}

    public record Result(boolean success, Refund refund, String error) {
        public static Result success(Refund refund) {
            return new Result(true, refund, null);
        }

        public static Result failure(String error) {
            return new Result(false, null, error);
        }
    }
}
