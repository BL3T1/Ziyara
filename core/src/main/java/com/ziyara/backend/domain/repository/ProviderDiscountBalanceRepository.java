package com.ziyara.backend.domain.repository;

import com.ziyara.backend.domain.entity.ProviderDiscountBalance;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

public interface ProviderDiscountBalanceRepository {

    Optional<ProviderDiscountBalance> findByProviderId(UUID providerId);

    Optional<ProviderDiscountBalance> lockByProviderId(UUID providerId);

    void debitSpent(UUID providerId, BigDecimal amount);

    void upsertAllocated(UUID providerId, BigDecimal grantAmount, String currency);

    void recordDebit(UUID providerId, UUID discountCodeId, BigDecimal amount, String description);
}
