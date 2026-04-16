package com.ziyara.backend.infrastructure.persistence.adapter;

import com.ziyara.backend.domain.entity.Refund;
import com.ziyara.backend.domain.enums.RefundStatus;
import com.ziyara.backend.domain.repository.RefundRepository;
import com.ziyara.backend.infrastructure.persistence.entity.RefundJpaEntity;
import com.ziyara.backend.infrastructure.persistence.mapper.RefundMapper;
import com.ziyara.backend.infrastructure.persistence.repository.RefundJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class RefundRepositoryAdapter implements RefundRepository {

    private final RefundJpaRepository jpaRepository;
    private final RefundMapper mapper;

    @Override
    public Refund save(Refund refund) {
        RefundJpaEntity entity = mapper.toJpaEntity(refund);
        return mapper.toDomainEntity(jpaRepository.save(entity));
    }

    @Override
    public Optional<Refund> findById(UUID id) {
        return jpaRepository.findById(id).map(mapper::toDomainEntity);
    }

    @Override
    public List<Refund> findByPaymentId(UUID paymentId) {
        return jpaRepository.findByPaymentId(paymentId).stream()
                .map(mapper::toDomainEntity)
                .collect(Collectors.toList());
    }

    @Override
    public List<Refund> findByBookingId(UUID bookingId) {
        return List.of();
    }

    @Override
    public List<Refund> findByStatus(RefundStatus status) {
        return jpaRepository.findByStatus(status).stream()
                .map(mapper::toDomainEntity)
                .collect(Collectors.toList());
    }

    @Override
    public long count() {
        return jpaRepository.count();
    }
}
