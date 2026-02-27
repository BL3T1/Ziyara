package com.ziyarah.infrastructure.persistence.repository;

import com.ziyarah.infrastructure.persistence.entity.ExchangeRateJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Spring Data JPA Repository: ExchangeRateJpaRepository
 */
@Repository
public interface ExchangeRateJpaRepository extends JpaRepository<ExchangeRateJpaEntity, UUID> {
    Optional<ExchangeRateJpaEntity> findByFromCurrencyAndToCurrency(String from, String to);
    List<ExchangeRateJpaEntity> findByFromCurrency(String from);
}
