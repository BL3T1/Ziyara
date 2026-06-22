package com.ziyara.backend.domain.repository;

import com.ziyara.backend.domain.entity.ExchangeRate;
import java.math.BigDecimal;
import java.time.LocalDate;
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

    /**
     * Insert or update a rate for the given currency pair and effective date.
     * Uses PostgreSQL {@code ON CONFLICT ... DO UPDATE} — idempotent on repeated calls for the same date.
     */
    void upsert(String fromCurrency, String toCurrency, BigDecimal rate, LocalDate effectiveDate);
}
