package com.ziyarah.infrastructure.persistence.mapper;

import com.ziyarah.domain.entity.ExchangeRate;
import com.ziyarah.infrastructure.persistence.entity.ExchangeRateJpaEntity;
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
        exchangeRate.setProvider(entity.getProvider());
        exchangeRate.setEffectiveAt(entity.getEffectiveAt());
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
                .provider(exchangeRate.getProvider())
                .effectiveAt(exchangeRate.getEffectiveAt())
                .createdAt(exchangeRate.getCreatedAt())
                .updatedAt(exchangeRate.getUpdatedAt())
                .build();
    }
}
