package com.ziyara.backend.infrastructure.persistence.mapper;

import com.ziyara.backend.domain.entity.CashCollection;
import com.ziyara.backend.infrastructure.persistence.entity.CashCollectionJpaEntity;
import org.springframework.stereotype.Component;

@Component
public class CashCollectionMapper {

    public CashCollection toDomainEntity(CashCollectionJpaEntity entity) {
        if (entity == null) return null;
        CashCollection c = new CashCollection();
        c.setId(entity.getId());
        c.setPaymentId(entity.getPaymentId());
        c.setProviderId(entity.getProviderId());
        c.setCollectedAt(entity.getCollectedAt());
        c.setCollectedByUserId(entity.getCollectedByUserId());
        c.setAmount(entity.getAmount());
        c.setCurrency(entity.getCurrency() != null ? entity.getCurrency() : "USD");
        c.setReceiptNumber(entity.getReceiptNumber());
        c.setNotes(entity.getNotes());
        c.setReconciledAt(entity.getReconciledAt());
        c.setReconciledByUserId(entity.getReconciledByUserId());
        c.setStatus(entity.getStatus());
        c.setCreatedAt(entity.getCreatedAt());
        c.setUpdatedAt(entity.getUpdatedAt());
        return c;
    }

    public CashCollectionJpaEntity toJpaEntity(CashCollection c) {
        if (c == null) return null;
        return CashCollectionJpaEntity.builder()
                .id(c.getId())
                .paymentId(c.getPaymentId())
                .providerId(c.getProviderId())
                .collectedAt(c.getCollectedAt())
                .collectedByUserId(c.getCollectedByUserId())
                .amount(c.getAmount())
                .currency(c.getCurrency())
                .receiptNumber(c.getReceiptNumber())
                .notes(c.getNotes())
                .reconciledAt(c.getReconciledAt())
                .reconciledByUserId(c.getReconciledByUserId())
                .status(c.getStatus())
                .createdAt(c.getCreatedAt())
                .updatedAt(c.getUpdatedAt())
                .build();
    }
}
