package com.ziyara.backend.domain.usecase.refund;

import com.ziyara.backend.domain.entity.Payment;
import com.ziyara.backend.domain.entity.Refund;
import com.ziyara.backend.domain.repository.PaymentRepository;
import com.ziyara.backend.domain.repository.RefundRepository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public class RequestRefundUseCase {

    private final PaymentRepository paymentRepository;
    private final RefundRepository refundRepository;

    public RequestRefundUseCase(PaymentRepository paymentRepository, RefundRepository refundRepository) {
        this.paymentRepository = paymentRepository;
        this.refundRepository = refundRepository;
    }

    public Result execute(Input input) {
        Optional<Payment> paymentOpt = paymentRepository.findById(input.paymentId());
        if (paymentOpt.isEmpty()) {
            return Result.failure("Payment not found");
        }

        Payment payment = paymentOpt.get();

        if (!payment.isSuccessful()) {
            return Result.failure("Refunds can only be requested against completed payments");
        }

        if (input.amount().compareTo(BigDecimal.ZERO) <= 0) {
            return Result.failure("Refund amount must be positive");
        }

        if (input.amount().compareTo(payment.getAmount()) > 0) {
            return Result.failure("Refund amount cannot exceed the original payment amount of "
                    + payment.getAmount() + " " + payment.getCurrency());
        }

        // Guard against refunding more than the original charge across multiple partial refunds
        List<Refund> existingRefunds = refundRepository.findByPaymentId(input.paymentId());
        BigDecimal alreadyRefunded = existingRefunds.stream()
                .map(Refund::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        if (alreadyRefunded.add(input.amount()).compareTo(payment.getAmount()) > 0) {
            return Result.failure("Total refund would exceed the original payment amount");
        }

        Refund refund = new Refund();
        refund.setPaymentId(input.paymentId());
        refund.setAmount(input.amount());
        refund.setCurrency(payment.getCurrency());
        refund.setReason(input.reason());
        refund.setProcessedBy(input.requestedBy());

        Refund saved = refundRepository.save(refund);
        return Result.success(saved);
    }

    public record Input(UUID paymentId, BigDecimal amount, String reason, UUID requestedBy) {}

    public record Result(boolean success, Refund refund, String error) {
        public static Result success(Refund refund) {
            return new Result(true, refund, null);
        }

        public static Result failure(String error) {
            return new Result(false, null, error);
        }
    }
}
