package com.ziyara.backend.domain.usecase.discount;

import com.ziyara.backend.domain.entity.DiscountCode;
import com.ziyara.backend.domain.repository.DiscountCodeRepository;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

public class ApproveDiscountCodeUseCase {

    private final DiscountCodeRepository discountCodeRepository;

    public ApproveDiscountCodeUseCase(DiscountCodeRepository discountCodeRepository) {
        this.discountCodeRepository = discountCodeRepository;
    }

    public Result execute(Input input) {
        Optional<DiscountCode> codeOpt = discountCodeRepository.findById(input.discountCodeId());
        if (codeOpt.isEmpty()) {
            return Result.failure("Discount code not found");
        }

        DiscountCode discountCode = codeOpt.get();

        if (!"PENDING_APPROVAL".equals(discountCode.getApprovalStatus())
                && !"DRAFT".equals(discountCode.getApprovalStatus())) {
            return Result.failure("Discount code is not awaiting approval. Current status: "
                    + discountCode.getApprovalStatus());
        }

        if (input.approve()) {
            discountCode.setApprovalStatus("APPROVED");
            discountCode.setApprovedBy(input.reviewedBy());
            discountCode.setApprovedAt(LocalDateTime.now());
        } else {
            discountCode.setApprovalStatus("REJECTED");
        }

        DiscountCode saved = discountCodeRepository.save(discountCode);
        return Result.success(saved);
    }

    public record Input(UUID discountCodeId, boolean approve, UUID reviewedBy) {}

    public record Result(boolean success, DiscountCode discountCode, String error) {
        public static Result success(DiscountCode discountCode) {
            return new Result(true, discountCode, null);
        }

        public static Result failure(String error) {
            return new Result(false, null, error);
        }
    }
}
