package com.ziyara.backend.infrastructure.persistence.mapper;

import com.ziyara.backend.domain.entity.Payment;
import com.ziyara.backend.infrastructure.persistence.entity.PaymentJpaEntity;
import org.springframework.stereotype.Component;

/**
 * Mapper: PaymentMapper
 */
@Component
public class PaymentMapper {
    
    public Payment toDomainEntity(PaymentJpaEntity entity) {
        if (entity == null) {
            return null;
        }
        
        Payment payment = new Payment();
        payment.setId(entity.getId());
        payment.setBookingId(entity.getBookingId());
        payment.setAmount(entity.getAmount());
        payment.setCurrency(entity.getCurrency() != null ? entity.getCurrency() : "USD");
        payment.setMethod(entity.getMethod());
        payment.setStatus(entity.getStatus());
        payment.setTransactionReference(entity.getTransactionRef());
        payment.setGatewayReference(entity.getGatewayReference());
        payment.setThreeDsStatus(entity.getThreeDsStatus());
        payment.setGatewayResponse(entity.getGatewayResponse());
        payment.setGatewayName(entity.getGatewayName());
        payment.setPaymentToken(entity.getPaymentToken());
        payment.setIdempotencyKey(entity.getIdempotencyKey());
        payment.setErrorMessage(entity.getErrorMessage());
        payment.setProcessedAt(entity.getProcessedAt());
        payment.setCreatedAt(entity.getCreatedAt());
        payment.setUpdatedAt(entity.getUpdatedAt());
        
        return payment;
    }
    
    public PaymentJpaEntity toJpaEntity(Payment payment) {
        if (payment == null) {
            return null;
        }
        
        return PaymentJpaEntity.builder()
                .id(payment.getId())
                .bookingId(payment.getBookingId())
                .amount(payment.getAmount())
                .currency(payment.getCurrency())
                .method(payment.getMethod())
                .status(payment.getStatus())
                .transactionRef(payment.getTransactionReference())
                .gatewayReference(payment.getGatewayReference())
                .threeDsStatus(payment.getThreeDsStatus())
                .gatewayResponse(payment.getGatewayResponse())
                .gatewayName(payment.getGatewayName())
                .paymentToken(payment.getPaymentToken())
                .idempotencyKey(payment.getIdempotencyKey())
                .errorMessage(payment.getErrorMessage())
                .processedAt(payment.getProcessedAt())
                .createdAt(payment.getCreatedAt())
                .updatedAt(payment.getUpdatedAt())
                .build();
    }
}
