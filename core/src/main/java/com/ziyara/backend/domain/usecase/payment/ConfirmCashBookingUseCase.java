package com.ziyara.backend.domain.usecase.payment;

import com.ziyara.backend.domain.entity.Payment;
import com.ziyara.backend.domain.enums.PaymentMethod;
import com.ziyara.backend.domain.repository.PaymentRepository;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

/**
 * Creates a PENDING cash payment for a booking without calling any gateway.
 * Idempotent on idempotencyKey and one-completed-payment-per-booking.
 */
public class ConfirmCashBookingUseCase {

    private final PaymentRepository paymentRepository;

    public ConfirmCashBookingUseCase(PaymentRepository paymentRepository) {
        this.paymentRepository = paymentRepository;
    }

    public Result execute(Input input) {
        if (input.bookingId() == null) {
            return Result.failure("bookingId is required");
        }
        if (input.amount() == null || input.amount().signum() <= 0) {
            return Result.failure("amount must be positive");
        }

        if (input.idempotencyKey() != null) {
            Optional<Payment> existing = paymentRepository.findByIdempotencyKey(input.idempotencyKey());
            if (existing.isPresent()) {
                return Result.success(existing.get(), true);
            }
        }

        Optional<Payment> bookingPayment = paymentRepository.findByBookingId(input.bookingId());
        if (bookingPayment.isPresent() && bookingPayment.get().isSuccessful()) {
            return Result.failure("A completed payment already exists for this booking");
        }

        Payment payment = new Payment();
        payment.setBookingId(input.bookingId());
        payment.setAmount(input.amount());
        payment.setCurrency(input.currency() != null ? input.currency() : "USD");
        payment.setMethod(PaymentMethod.CASH);
        payment.setIdempotencyKey(input.idempotencyKey());

        Payment saved = paymentRepository.save(payment);
        return Result.success(saved, false);
    }

    public record Input(
            UUID bookingId,
            BigDecimal amount,
            String currency,
            String idempotencyKey
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
