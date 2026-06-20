package com.ziyara.backend.domain.repository;

import com.ziyara.backend.domain.common.PageQuery;
import com.ziyara.backend.domain.common.PagedResult;
import com.ziyara.backend.domain.entity.CashCollection;
import com.ziyara.backend.domain.enums.CashCollectionStatus;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository Port: CashCollectionRepository
 */
public interface CashCollectionRepository {

    CashCollection save(CashCollection collection);

    Optional<CashCollection> findById(UUID id);

    Optional<CashCollection> findByReceiptNumber(String receiptNumber);

    List<CashCollection> findByPaymentId(UUID paymentId);

    PagedResult<CashCollection> findByProviderId(UUID providerId, PageQuery pageQuery);

    PagedResult<CashCollection> findByStatus(CashCollectionStatus status, PageQuery pageQuery);

    List<CashCollection> findOpenForProvider(UUID providerId);

    /** Sum of OPEN cash collections for the provider — used by payout balance calc. */
    BigDecimal sumOpenForProvider(UUID providerId);

    /** Collections recorded by a provider on the given calendar day (UTC). */
    List<CashCollection> findByProviderIdAndDay(UUID providerId, LocalDate day);

    /** Next sequence value for receipt numbering. Backed by Postgres sequence. */
    long nextReceiptSequence();
}
