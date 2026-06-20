package com.ziyara.backend.infrastructure.persistence.repository;

import com.ziyara.backend.infrastructure.persistence.entity.ExchangeRateJpaEntity;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Spring Data JPA Repository: ExchangeRateJpaRepository
 */
@Repository
public interface ExchangeRateJpaRepository extends JpaRepository<ExchangeRateJpaEntity, UUID> {
    @Query("SELECT e FROM ExchangeRateJpaEntity e WHERE e.fromCurrency = :from AND e.toCurrency = :to AND e.effectiveDate <= :date ORDER BY e.effectiveDate DESC")
    List<ExchangeRateJpaEntity> findByFromCurrencyAndToCurrencyAndEffectiveDateBefore(
            @Param("from") String from, @Param("to") String to, @Param("date") LocalDate date, Pageable pageable);

    default Optional<ExchangeRateJpaEntity> findByFromCurrencyAndToCurrency(String from, String to) {
        List<ExchangeRateJpaEntity> list = findByFromCurrencyAndToCurrencyAndEffectiveDateBefore(
                from, to, LocalDate.now(), Pageable.ofSize(1));
        return list.isEmpty() ? Optional.empty() : Optional.of(list.get(0));
    }

    List<ExchangeRateJpaEntity> findByFromCurrency(String from);

    @Modifying
    @Transactional
    @Query(value = """
            INSERT INTO pay_exchange_rates (from_currency, to_currency, rate, effective_date)
            VALUES (:from, :to, :rate, :date)
            ON CONFLICT (from_currency, to_currency, effective_date)
            DO UPDATE SET rate = EXCLUDED.rate, updated_at = NOW()
            """, nativeQuery = true)
    void upsertRate(@Param("from") String from,
                    @Param("to") String to,
                    @Param("rate") BigDecimal rate,
                    @Param("date") LocalDate date);
}
