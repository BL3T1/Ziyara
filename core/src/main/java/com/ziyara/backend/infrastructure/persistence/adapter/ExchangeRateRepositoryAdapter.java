package com.ziyara.backend.infrastructure.persistence.adapter;

import com.ziyara.backend.domain.entity.ExchangeRate;
import com.ziyara.backend.domain.repository.ExchangeRateRepository;
import com.ziyara.backend.infrastructure.persistence.entity.ExchangeRateJpaEntity;
import com.ziyara.backend.infrastructure.persistence.mapper.ExchangeRateMapper;
import com.ziyara.backend.infrastructure.persistence.repository.ExchangeRateJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Repository Adapter: ExchangeRateRepositoryAdapter
 */
@Component
@RequiredArgsConstructor
public class ExchangeRateRepositoryAdapter implements ExchangeRateRepository {
    
    private final ExchangeRateJpaRepository exchangeRateJpaRepository;
    private final ExchangeRateMapper exchangeRateMapper;
    
    @Override
    public ExchangeRate save(ExchangeRate exchangeRate) {
        ExchangeRateJpaEntity entity = exchangeRateMapper.toJpaEntity(exchangeRate);
        ExchangeRateJpaEntity savedEntity = exchangeRateJpaRepository.save(entity);
        return exchangeRateMapper.toDomainEntity(savedEntity);
    }
    
    @Override
    public Optional<ExchangeRate> findById(UUID id) {
        return exchangeRateJpaRepository.findById(id)
                .map(exchangeRateMapper::toDomainEntity);
    }
    
    @Override
    public Optional<ExchangeRate> findByFromCurrencyAndToCurrency(String from, String to) {
        return exchangeRateJpaRepository.findByFromCurrencyAndToCurrency(from, to)
                .map(exchangeRateMapper::toDomainEntity);
    }
    
    @Override
    public List<ExchangeRate> findByFromCurrency(String from) {
        return exchangeRateJpaRepository.findByFromCurrency(from).stream()
                .map(exchangeRateMapper::toDomainEntity)
                .collect(Collectors.toList());
    }
    
    @Override
    public List<ExchangeRate> findAll() {
        return exchangeRateJpaRepository.findAll().stream()
                .map(exchangeRateMapper::toDomainEntity)
                .collect(Collectors.toList());
    }
    
    @Override
    public void deleteById(UUID id) {
        exchangeRateJpaRepository.deleteById(id);
    }

    @Override
    @Transactional
    public void upsert(String fromCurrency, String toCurrency, BigDecimal rate, LocalDate effectiveDate) {
        exchangeRateJpaRepository.upsertRate(fromCurrency, toCurrency, rate, effectiveDate);
    }
}
