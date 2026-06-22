package com.ziyara.backend.domain.usecase.payment;

import com.ziyara.backend.domain.entity.Payment;
import com.ziyara.backend.domain.enums.PaymentStatus;
import com.ziyara.backend.domain.repository.PaymentRepository;

import java.util.Optional;
import java.util.UUID;

public class FailPaymentUseCase {

    private final PaymentRepository paymentRepository;

    public FailPaymentUseCase(PaymentRepository paymentRepository) {
        this.paymentRepository = paymentRepository;
    }

    public Result execute(Input input) {
        Optional<Payment> paymentOpt = paymentRepository.findById(input.paymentId());
        if (paymentOpt.isEmpty()) {
            return Result.failure("Payment not found");
        }

        Payment payment = paymentOpt.get();

        if (payment.getStatus() != PaymentStatus.PENDING) {
            return Result.failure("Only PENDING payments can be failed. Current status: " + payment.getStatus());
        }

        payment.fail(input.errorMessage());
        if (input.gatewayResponse() != null) {
            payment.setGatewayResponse(input.gatewayResponse());
        }

        Payment saved = paymentRepository.save(payment);
        return Result.success(saved);
    }

    public record Input(UUID paymentId, String errorMessage, String gatewayResponse) {}

    public record Result(boolean success, Payment payment, String error) {
        public static Result success(Payment payment) {
            return new Result(true, payment, null);
        }

        public static Result failure(String error) {
            return new Result(false, null, error);
        }
    }
}
