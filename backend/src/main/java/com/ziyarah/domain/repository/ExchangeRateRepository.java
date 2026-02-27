package com.ziyarah.domain.repository;

import com.ziyarah.domain.entity.ExchangeRate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository Port: ExchangeRateRepository
 */
public interface ExchangeRateRepository {
    ExchangeRate save(ExchangeRate exchangeRate);
    Optional<ExchangeRate> findById(UUID id);
    Optional<ExchangeRate> findByFromCurrencyAndToCurrency(String from, String to);
    List<ExchangeRate> findByFromCurrency(String from);
    List<ExchangeRate> findAll();
    void deleteById(UUID id);
}
