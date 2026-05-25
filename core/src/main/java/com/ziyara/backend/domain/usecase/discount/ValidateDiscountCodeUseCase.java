package com.ziyara.backend.domain.usecase.discount;

import com.ziyara.backend.domain.entity.DiscountCode;
import com.ziyara.backend.domain.repository.DiscountCodeRepository;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

public class ValidateDiscountCodeUseCase {

    private final DiscountCodeRepository discountCodeRepository;

    public ValidateDiscountCodeUseCase(DiscountCodeRepository discountCodeRepository) {
        this.discountCodeRepository = discountCodeRepository;
    }

    public Result execute(Input input) {
        Optional<DiscountCode> codeOpt = discountCodeRepository.findByCode(input.code());
        if (codeOpt.isEmpty()) {
            return Result.failure("Discount code not found");
        }

        DiscountCode discountCode = codeOpt.get();

        if (!discountCode.isValid()) {
            return Result.failure("Discount code is not valid or has expired");
        }

        if (!"APPROVED".equals(discountCode.getApprovalStatus())) {
            return Result.failure("Discount code has not been approved for use");
        }

        // Check provider scope if restricted
        if (discountCode.getProviderId() != null && input.providerId() != null
                && !discountCode.getProviderId().equals(input.providerId())) {
            return Result.failure("Discount code is not valid for this provider");
        }

        // Check service scope if restricted
        if (discountCode.getApplicableServiceIds() != null
                && !discountCode.getApplicableServiceIds().isEmpty()
                && input.serviceId() != null
                && !discountCode.getApplicableServiceIds().contains(input.serviceId())) {
            return Result.failure("Discount code is not applicable to this service");
        }

        BigDecimal discountAmount = discountCode.calculateDiscount(input.bookingAmount());

        if (discountAmount.compareTo(BigDecimal.ZERO) == 0) {
            return Result.failure("Booking amount does not meet the minimum requirement of "
                    + discountCode.getMinBookingAmount());
        }

        return Result.success(discountCode, discountAmount);
    }

    public record Input(String code, BigDecimal bookingAmount, UUID serviceId, UUID providerId) {}

    public record Result(boolean success, DiscountCode discountCode, BigDecimal discountAmount, String error) {
        public static Result success(DiscountCode discountCode, BigDecimal discountAmount) {
            return new Result(true, discountCode, discountAmount, null);
        }

        public static Result failure(String error) {
            return new Result(false, null, BigDecimal.ZERO, error);
        }
    }
}
