package com.ziyara.backend.domain.usecase.payment;

import com.ziyara.backend.domain.entity.Payment;
import com.ziyara.backend.domain.enums.PaymentStatus;
import com.ziyara.backend.domain.repository.PaymentRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Booking no-show: any COMPLETED bank-transfer deposits on the booking are
 * reclassified as NO_SHOW_FORFEIT. PENDING cash payments are cancelled.
 */
public class ForfeitNoShowDepositUseCase {

    private final PaymentRepository paymentRepository;

    public ForfeitNoShowDepositUseCase(PaymentRepository paymentRepository) {
        this.paymentRepository = paymentRepository;
    }

    public Result execute(Input input) {
        if (input.bookingId() == null) return Result.failure("bookingId is required");

        List<Payment> payments = paymentRepository.findAllByBookingId(input.bookingId());
        if (payments.isEmpty()) return Result.success(List.of());

        java.util.ArrayList<Payment> updated = new java.util.ArrayList<>();
        for (Payment p : payments) {
            if (p.getStatus() == PaymentStatus.COMPLETED) {
                p.setStatus(PaymentStatus.NO_SHOW_FORFEIT);
                p.setProcessedAt(LocalDateTime.now());
                updated.add(paymentRepository.save(p));
            } else if (p.getStatus() == PaymentStatus.PENDING) {
                p.setStatus(PaymentStatus.CANCELLED);
                p.setErrorMessage("No-show cancellation");
                p.setProcessedAt(LocalDateTime.now());
                updated.add(paymentRepository.save(p));
            }
        }
        return Result.success(updated);
    }

    public record Input(UUID bookingId, UUID adminUserId) {}

    public record Result(boolean success, List<Payment> updatedPayments, String error) {
        public static Result success(List<Payment> updated) {
            return new Result(true, updated, null);
        }
        public static Result failure(String error) {
            return new Result(false, List.of(), error);
        }
    }
}
