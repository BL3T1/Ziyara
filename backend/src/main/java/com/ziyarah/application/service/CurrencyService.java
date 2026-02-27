package com.ziyarah.application.service;

import com.ziyarah.application.dto.response.ExchangeRateResponse;
import com.ziyarah.domain.entity.ExchangeRate;
import com.ziyarah.domain.repository.ExchangeRateRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingByMode;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Service: CurrencyService
 * Handles currency conversion and exchange rates
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class CurrencyService {
    
    private final ExchangeRateRepository exchangeRateRepository;
    
    @Transactional(readOnly = true)
    public BigDecimal convert(BigDecimal amount, String from, String to) {
        if (from.equalsIgnoreCase(to)) return amount;
        
        log.debug("Converting {} from {} to {}", amount, from, to);
        
        ExchangeRate rate = exchangeRateRepository.findByFromCurrencyAndToCurrency(from, to)
                .orElseThrow(() -> new RuntimeException("Exchange rate not found for " + from + " to " + to));
        
        return amount.multiply(rate.getRate());
    }
    
    @Transactional(readOnly = true)
    public List<ExchangeRateResponse> getAllRates() {
        return exchangeRateRepository.findAll().stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }
    
    private ExchangeRateResponse mapToResponse(ExchangeRate rate) {
        return ExchangeRateResponse.builder()
                .id(rate.getId())
                .fromCurrency(rate.getFromCurrency())
                .toCurrency(rate.getToCurrency())
                .rate(rate.getRate())
                .provider(rate.getProvider())
                .effectiveAt(rate.getEffectiveAt())
                .build();
    }
}
