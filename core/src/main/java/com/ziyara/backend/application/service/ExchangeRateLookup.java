package com.ziyara.backend.application.service;

import com.ziyara.backend.domain.entity.ExchangeRate;
import com.ziyara.backend.domain.repository.ExchangeRateRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

/**
 * Proxied delegate for cached exchange-rate lookups.
 * Extracted from CurrencyService to avoid Spring self-invocation cache bypass:
 * CurrencyService.convert() calling this.getCachedRate() would skip the proxy
 * and miss the cache entirely; injecting this component routes the call through
 * the AOP proxy so the exchangeRates cache is actually populated.
 */
@Component
@RequiredArgsConstructor
public class ExchangeRateLookup {

    private final ExchangeRateRepository exchangeRateRepository;

    @Cacheable(value = "exchangeRates", key = "#from.toUpperCase() + '_' + #to.toUpperCase()")
    @Transactional(readOnly = true)
    public Optional<ExchangeRate> getCachedRate(String from, String to) {
        return exchangeRateRepository.findByFromCurrencyAndToCurrency(from, to);
    }
}
