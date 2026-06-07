package com.ziyara.backend.infrastructure.persistence.adapter;

import com.ziyara.backend.domain.common.PageQuery;
import com.ziyara.backend.domain.common.PagedResult;
import com.ziyara.backend.domain.entity.Payment;
import com.ziyara.backend.domain.enums.PaymentStatus;
import java.math.BigDecimal;
import com.ziyara.backend.domain.repository.PaymentRepository;
import com.ziyara.backend.infrastructure.persistence.entity.PaymentJpaEntity;
import com.ziyara.backend.infrastructure.persistence.mapper.PaymentMapper;
import com.ziyara.backend.infrastructure.persistence.repository.PaymentJpaRepository;
import com.ziyara.backend.infrastructure.persistence.util.PageConverter;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
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
    public List<Payment> findAllByBookingId(UUID bookingId) {
        return paymentJpaRepository.findAllByBookingIdOrderByCreatedAtAsc(bookingId)
                .stream().map(paymentMapper::toDomainEntity).toList();
    }
    
    @Override
    public Optional<Payment> findByTransactionReference(String reference) {
        return paymentJpaRepository.findByTransactionRef(reference)
                .map(paymentMapper::toDomainEntity);
    }

    @Override
    public Optional<Payment> findByGatewayReference(String gatewayReference) {
        return paymentJpaRepository.findByGatewayReference(gatewayReference)
                .map(paymentMapper::toDomainEntity);
    }

    @Override
    public Optional<Payment> findByIdempotencyKey(String idempotencyKey) {
        return paymentJpaRepository.findByIdempotencyKey(idempotencyKey)
                .map(paymentMapper::toDomainEntity);
    }
    
    @Override
    public List<Payment> findByStatus(PaymentStatus status) {
        return paymentJpaRepository.findByStatus(status).stream()
                .map(paymentMapper::toDomainEntity)
                .collect(Collectors.toList());
    }

    @Override
    public PagedResult<Payment> findAll(PageQuery pageQuery) {
        return PageConverter.toPagedResult(paymentJpaRepository.findAll(PageConverter.toPageable(pageQuery)), paymentMapper::toDomainEntity);
    }

    @Override
    public PagedResult<Payment> findByStatus(PaymentStatus status, PageQuery pageQuery) {
        return PageConverter.toPagedResult(paymentJpaRepository.findByStatus(status, PageConverter.toPageable(pageQuery)), paymentMapper::toDomainEntity);
    }

    @Override
    public PagedResult<Payment> findByCustomerUserId(UUID customerUserId, PageQuery pageQuery) {
        return PageConverter.toPagedResult(paymentJpaRepository.findByBookingCustomerUserId(customerUserId, PageConverter.toPageable(pageQuery)), paymentMapper::toDomainEntity);
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

    @Override
    public BigDecimal sumCompletedAmountBetween(LocalDateTime from, LocalDateTime to) {
        return paymentJpaRepository.sumCompletedAmountBetween(PaymentStatus.COMPLETED, from, to);
    }

    @Override
    public BigDecimal sumCompletedAmountByBookingIds(List<UUID> bookingIds) {
        if (bookingIds == null || bookingIds.isEmpty()) return BigDecimal.ZERO;
        BigDecimal result = paymentJpaRepository.sumCompletedAmountByBookingIds(PaymentStatus.COMPLETED, bookingIds);
        return result != null ? result : BigDecimal.ZERO;
    }

    @Override
    public BigDecimal sumByStatus(PaymentStatus status) {
        BigDecimal result = paymentJpaRepository.sumByStatus(status);
        return result != null ? result : BigDecimal.ZERO;
    }

    @Override
    public List<Payment> findCompletedByBookingIdsSince(List<UUID> bookingIds, LocalDateTime since) {
        if (bookingIds == null || bookingIds.isEmpty()) return List.of();
        return paymentJpaRepository
                .findCompletedByBookingIdsSince(PaymentStatus.COMPLETED, bookingIds, since)
                .stream()
                .map(paymentMapper::toDomainEntity)
                .collect(Collectors.toList());
    }
}
