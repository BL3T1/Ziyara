package com.ziyara.backend.infrastructure.persistence.adapter;

import com.ziyara.backend.domain.common.PageQuery;
import com.ziyara.backend.domain.common.PagedResult;
import com.ziyara.backend.domain.entity.CashCollection;
import com.ziyara.backend.domain.enums.CashCollectionStatus;
import com.ziyara.backend.domain.repository.CashCollectionRepository;
import com.ziyara.backend.infrastructure.persistence.entity.CashCollectionJpaEntity;
import com.ziyara.backend.infrastructure.persistence.mapper.CashCollectionMapper;
import com.ziyara.backend.infrastructure.persistence.repository.CashCollectionJpaRepository;
import com.ziyara.backend.infrastructure.persistence.util.PageConverter;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class CashCollectionRepositoryAdapter implements CashCollectionRepository {

    private final CashCollectionJpaRepository jpaRepository;
    private final CashCollectionMapper mapper;

    @Override
    public CashCollection save(CashCollection collection) {
        CashCollectionJpaEntity entity = mapper.toJpaEntity(collection);
        return mapper.toDomainEntity(jpaRepository.save(entity));
    }

    @Override
    public Optional<CashCollection> findById(UUID id) {
        return jpaRepository.findById(id).map(mapper::toDomainEntity);
    }

    @Override
    public Optional<CashCollection> findByReceiptNumber(String receiptNumber) {
        return jpaRepository.findByReceiptNumber(receiptNumber).map(mapper::toDomainEntity);
    }

    @Override
    public List<CashCollection> findByPaymentId(UUID paymentId) {
        return jpaRepository.findByPaymentId(paymentId).stream().map(mapper::toDomainEntity).toList();
    }

    @Override
    public PagedResult<CashCollection> findByProviderId(UUID providerId, PageQuery pageQuery) {
        return PageConverter.toPagedResult(
                jpaRepository.findByProviderId(providerId, PageConverter.toPageable(pageQuery)),
                mapper::toDomainEntity);
    }

    @Override
    public PagedResult<CashCollection> findByStatus(CashCollectionStatus status, PageQuery pageQuery) {
        return PageConverter.toPagedResult(
                jpaRepository.findByStatus(status, PageConverter.toPageable(pageQuery)),
                mapper::toDomainEntity);
    }

    @Override
    public List<CashCollection> findOpenForProvider(UUID providerId) {
        return jpaRepository.findByProviderIdAndStatus(providerId, CashCollectionStatus.OPEN)
                .stream().map(mapper::toDomainEntity).toList();
    }

    @Override
    public BigDecimal sumOpenForProvider(UUID providerId) {
        BigDecimal sum = jpaRepository.sumOpenForProvider(providerId);
        return sum != null ? sum : BigDecimal.ZERO;
    }

    @Override
    public List<CashCollection> findByProviderIdAndDay(UUID providerId, LocalDate day) {
        LocalDateTime from = day.atStartOfDay();
        LocalDateTime to = day.plusDays(1).atStartOfDay();
        return jpaRepository.findByProviderIdInRange(providerId, from, to)
                .stream().map(mapper::toDomainEntity).toList();
    }

    @Override
    public long nextReceiptSequence() {
        return jpaRepository.nextReceiptSequence();
    }
}
