package com.ziyara.backend.application.service;

import com.ziyara.backend.domain.entity.DiscountCode;
import com.ziyara.backend.domain.repository.DiscountCodeRepository;
import com.ziyara.backend.application.exception.BusinessException;
import com.ziyara.backend.application.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Approval state machine for discount codes: DRAFT â†’ PENDING_APPROVAL â†’ APPROVED | REJECTED.
 */
@Service
@RequiredArgsConstructor
public class DiscountApprovalService {

    private final DiscountCodeRepository discountCodeRepository;

    @Transactional
    public DiscountCode submitForApproval(UUID discountId) {
        DiscountCode code = findOrThrow(discountId);
        if (!"DRAFT".equals(code.getApprovalStatus())) {
            throw new BusinessException("Only DRAFT discounts can be submitted for approval");
        }
        code.setApprovalStatus("PENDING_APPROVAL");
        return discountCodeRepository.save(code);
    }

    @Transactional
    public DiscountCode approve(UUID discountId, UUID approvedBy) {
        DiscountCode code = findOrThrow(discountId);
        if (!"PENDING_APPROVAL".equals(code.getApprovalStatus())) {
            throw new BusinessException("Only PENDING_APPROVAL discounts can be approved");
        }
        code.setApprovalStatus("APPROVED");
        code.setApprovedBy(approvedBy);
        code.setApprovedAt(LocalDateTime.now());
        return discountCodeRepository.save(code);
    }

    @Transactional
    public DiscountCode reject(UUID discountId, UUID rejectedBy) {
        DiscountCode code = findOrThrow(discountId);
        if (!"PENDING_APPROVAL".equals(code.getApprovalStatus())) {
            throw new BusinessException("Only PENDING_APPROVAL discounts can be rejected");
        }
        code.setApprovalStatus("REJECTED");
        code.setApprovedBy(rejectedBy);
        code.setApprovedAt(LocalDateTime.now());
        return discountCodeRepository.save(code);
    }

    private DiscountCode findOrThrow(UUID id) {
        return discountCodeRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Discount code not found: " + id));
    }
}
