package com.ziyara.backend.application.service;

import com.ziyara.backend.application.dto.request.CreateExchangeRateRequest;
import com.ziyara.backend.application.dto.request.UpdateExchangeRateRequest;
import com.ziyara.backend.application.dto.response.ExchangeRateResponse;
import com.ziyara.backend.domain.entity.ExchangeRate;
import com.ziyara.backend.domain.repository.ExchangeRateRepository;
import com.ziyara.backend.application.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Service: CurrencyService
 * Handles currency conversion and exchange rates
 */
@Service
@RequiredArgsConstructor
public class CurrencyService {

    private static final Logger log = LoggerFactory.getLogger(CurrencyService.class);
    private final ExchangeRateRepository exchangeRateRepository;

    @Transactional(readOnly = true)
    public BigDecimal convert(BigDecimal amount, String from, String to) {
        if (from.equalsIgnoreCase(to))
            return amount;

        log.debug("Converting {} from {} to {}", amount, from, to);

        ExchangeRate rate = getCachedRate(from, to)
                .orElseThrow(() -> new RuntimeException("Exchange rate not found for " + from + " to " + to));

        return amount.multiply(rate.getRate());
    }

    @Cacheable(value = "exchangeRates", key = "#from.toUpperCase() + '_' + #to.toUpperCase()")
    @Transactional(readOnly = true)
    public java.util.Optional<ExchangeRate> getCachedRate(String from, String to) {
        return exchangeRateRepository.findByFromCurrencyAndToCurrency(from, to);
    }

    @CacheEvict(value = "exchangeRates", allEntries = true)
    public void evictRateCache() {
        log.debug("Exchange rate cache evicted");
    }

    /**
     * Converts to {@code to} when a rate exists; otherwise returns {@code amount} unchanged (logs a warning).
     */
    @Transactional(readOnly = true)
    public BigDecimal convertOrKeep(BigDecimal amount, String from, String to) {
        if (amount == null) {
            return BigDecimal.ZERO;
        }
        if (from == null || to == null) {
            return amount;
        }
        if (from.equalsIgnoreCase(to)) {
            return amount;
        }
        try {
            return convert(amount, from, to);
        } catch (RuntimeException e) {
            log.warn("No exchange rate {} -> {}; leaving amount as-is", from, to);
            return amount;
        }
    }

    @Transactional(readOnly = true)
    public List<ExchangeRateResponse> getAllRates() {
        return exchangeRateRepository.findAll().stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    /** Phase 3: Create exchange rate. */
    @CacheEvict(value = "exchangeRates", allEntries = true)
    @Transactional
    public ExchangeRateResponse createRate(CreateExchangeRateRequest request) {
        ExchangeRate rate = new ExchangeRate();
        rate.setFromCurrency(request.getFromCurrency().trim().toUpperCase());
        rate.setToCurrency(request.getToCurrency().trim().toUpperCase());
        rate.setRate(request.getRate());
        rate.setEffectiveDate(request.getEffectiveDate() != null ? request.getEffectiveDate() : java.time.LocalDate.now());
        return mapToResponse(exchangeRateRepository.save(rate));
    }

    /** Phase 3: Update exchange rate. */
    @CacheEvict(value = "exchangeRates", allEntries = true)
    @Transactional
    public ExchangeRateResponse updateRate(java.util.UUID id, UpdateExchangeRateRequest request) {
        ExchangeRate rate = exchangeRateRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Exchange rate not found"));
        if (request.getRate() != null) rate.setRate(request.getRate());
        if (request.getEffectiveDate() != null) rate.setEffectiveDate(request.getEffectiveDate());
        return mapToResponse(exchangeRateRepository.save(rate));
    }

    @Transactional(readOnly = true)
    public ExchangeRateResponse getRate(java.util.UUID id) {
        return exchangeRateRepository.findById(id)
                .map(this::mapToResponse)
                .orElseThrow(() -> new ResourceNotFoundException("Exchange rate not found"));
    }

    @CacheEvict(value = "exchangeRates", allEntries = true)
    @Transactional
    public void deleteRate(java.util.UUID id) {
        if (exchangeRateRepository.findById(id).isEmpty()) {
            throw new ResourceNotFoundException("Exchange rate not found");
        }
        exchangeRateRepository.deleteById(id);
    }

    private ExchangeRateResponse mapToResponse(ExchangeRate rate) {
        return ExchangeRateResponse.builder()
                .id(rate.getId())
                .fromCurrency(rate.getFromCurrency())
                .toCurrency(rate.getToCurrency())
                .rate(rate.getRate())
                .effectiveDate(rate.getEffectiveDate())
                .build();
    }
}
