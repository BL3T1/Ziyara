package com.ziyara.backend.domain.usecase.payment;

import com.ziyara.backend.domain.entity.Payment;
import com.ziyara.backend.domain.enums.PaymentMethod;
import com.ziyara.backend.domain.enums.PaymentStatus;
import com.ziyara.backend.domain.repository.PaymentRepository;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

public class InitiatePaymentUseCase {

    private final PaymentRepository paymentRepository;

    public InitiatePaymentUseCase(PaymentRepository paymentRepository) {
        this.paymentRepository = paymentRepository;
    }

    public Result execute(Input input) {
        // Idempotency: return existing payment if key already used
        if (input.idempotencyKey() != null) {
            Optional<Payment> existing = paymentRepository.findByIdempotencyKey(input.idempotencyKey());
            if (existing.isPresent()) {
                return Result.success(existing.get(), true);
            }
        }

        // Prevent double-charging a booking that already has a completed payment
        if (input.bookingId() != null) {
            Optional<Payment> bookingPayment = paymentRepository.findByBookingId(input.bookingId());
            if (bookingPayment.isPresent() && bookingPayment.get().isSuccessful()) {
                return Result.failure("A completed payment already exists for this booking");
            }
        }

        Payment payment = new Payment();
        payment.setBookingId(input.bookingId());
        payment.setAmount(input.amount());
        payment.setCurrency(input.currency());
        payment.setMethod(input.method());
        payment.setIdempotencyKey(input.idempotencyKey());
        payment.setEntityType(input.entityType());
        payment.setEntityId(input.entityId());
        payment.setCategory(input.category());

        Payment saved = paymentRepository.save(payment);
        return Result.success(saved, false);
    }

    public record Input(
            UUID bookingId,
            BigDecimal amount,
            String currency,
            PaymentMethod method,
            String idempotencyKey,
            String entityType,
            UUID entityId,
            String category
    ) {}

    public record Result(boolean success, Payment payment, boolean wasIdempotent, String error) {
        public static Result success(Payment payment, boolean wasIdempotent) {
            return new Result(true, payment, wasIdempotent, null);
        }

        public static Result failure(String error) {
            return new Result(false, null, false, error);
        }
    }
}
