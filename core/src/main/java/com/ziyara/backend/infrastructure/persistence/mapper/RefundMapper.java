package com.ziyara.backend.infrastructure.persistence.mapper;

import com.ziyara.backend.domain.entity.Refund;
import com.ziyara.backend.infrastructure.persistence.entity.RefundJpaEntity;
import org.springframework.stereotype.Component;

/**
 * Mapper: RefundMapper
 */
@Component
public class RefundMapper {
    
    public Refund toDomainEntity(RefundJpaEntity entity) {
        if (entity == null) {
            return null;
        }
        
        Refund refund = new Refund();
        refund.setId(entity.getId());
        refund.setPaymentId(entity.getPaymentId());
        refund.setAmount(entity.getAmount());
        refund.setCurrency(entity.getCurrency() != null ? entity.getCurrency() : "USD");
        refund.setStatus(entity.getStatus());
        refund.setReason(entity.getReason());
        refund.setProcessedBy(entity.getProcessedBy());
        refund.setTransactionReference(entity.getTransactionReference());
        refund.setProcessedAt(entity.getProcessedAt());
        refund.setCreatedAt(entity.getCreatedAt());
        refund.setUpdatedAt(entity.getUpdatedAt());
        
        return refund;
    }
    
    public RefundJpaEntity toJpaEntity(Refund refund) {
        if (refund == null) {
            return null;
        }
        
        return RefundJpaEntity.builder()
                .id(refund.getId())
                .paymentId(refund.getPaymentId())
                .amount(refund.getAmount())
                .currency(refund.getCurrency())
                .status(refund.getStatus())
                .reason(refund.getReason())
                .processedBy(refund.getProcessedBy())
                .transactionReference(refund.getTransactionReference())
                .processedAt(refund.getProcessedAt())
                .createdAt(refund.getCreatedAt())
                .updatedAt(refund.getUpdatedAt())
                .build();
    }
}
