package com.ziyara.backend.infrastructure.persistence.mapper;

import com.ziyara.backend.domain.entity.ExchangeRate;
import com.ziyara.backend.infrastructure.persistence.entity.ExchangeRateJpaEntity;
import org.springframework.stereotype.Component;

/**
 * Mapper: ExchangeRateMapper
 */
@Component
public class ExchangeRateMapper {
    
    public ExchangeRate toDomainEntity(ExchangeRateJpaEntity entity) {
        if (entity == null) {
            return null;
        }
        
        ExchangeRate exchangeRate = new ExchangeRate();
        exchangeRate.setId(entity.getId());
        exchangeRate.setFromCurrency(entity.getFromCurrency());
        exchangeRate.setToCurrency(entity.getToCurrency());
        exchangeRate.setRate(entity.getRate());
        exchangeRate.setEffectiveDate(entity.getEffectiveDate());
        exchangeRate.setCreatedAt(entity.getCreatedAt());
        exchangeRate.setUpdatedAt(entity.getUpdatedAt());
        
        return exchangeRate;
    }
    
    public ExchangeRateJpaEntity toJpaEntity(ExchangeRate exchangeRate) {
        if (exchangeRate == null) {
            return null;
        }
        
        return ExchangeRateJpaEntity.builder()
                .id(exchangeRate.getId())
                .fromCurrency(exchangeRate.getFromCurrency())
                .toCurrency(exchangeRate.getToCurrency())
                .rate(exchangeRate.getRate())
                .effectiveDate(exchangeRate.getEffectiveDate())
                .createdAt(exchangeRate.getCreatedAt())
                .updatedAt(exchangeRate.getUpdatedAt())
                .build();
    }
}
