package com.ziyarah.infrastructure.persistence.adapter;

import com.ziyarah.domain.entity.Payment;
import com.ziyarah.domain.enums.PaymentStatus;
import com.ziyarah.domain.repository.PaymentRepository;
import com.ziyarah.infrastructure.persistence.entity.PaymentJpaEntity;
import com.ziyarah.infrastructure.persistence.mapper.PaymentMapper;
import com.ziyarah.infrastructure.persistence.repository.PaymentJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Repository Adapter: PaymentRepositoryAdapter
 */
@Component
@RequiredArgsConstructor
public class PaymentRepositoryAdapter implements PaymentRepository {
    
    private final PaymentJpaRepository paymentJpaRepository;
    private final PaymentMapper paymentMapper;
    
    @Override
    public Payment save(Payment payment) {
        PaymentJpaEntity entity = paymentMapper.toJpaEntity(payment);
        PaymentJpaEntity savedEntity = paymentJpaRepository.save(entity);
        return paymentMapper.toDomainEntity(savedEntity);
    }
    
    @Override
    public Optional<Payment> findById(UUID id) {
        return paymentJpaRepository.findById(id)
                .map(paymentMapper::toDomainEntity);
    }
    
    @Override
    public Optional<Payment> findByBookingId(UUID bookingId) {
        return paymentJpaRepository.findByBookingId(bookingId)
                .map(paymentMapper::toDomainEntity);
    }
    
    @Override
    public Optional<Payment> findByTransactionReference(String reference) {
        return paymentJpaRepository.findByTransactionReference(reference)
                .map(paymentMapper::toDomainEntity);
    }
    
    @Override
    public List<Payment> findByStatus(PaymentStatus status) {
        return paymentJpaRepository.findByStatus(status).stream()
                .map(paymentMapper::toDomainEntity)
                .collect(Collectors.toList());
    }
    
    @Override
    public List<Payment> findAll() {
        return paymentJpaRepository.findAll().stream()
                .map(paymentMapper::toDomainEntity)
                .collect(Collectors.toList());
    }
    
    @Override
    public void deleteById(UUID id) {
        paymentJpaRepository.deleteById(id);
    }
}
