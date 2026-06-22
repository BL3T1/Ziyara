package com.ziyara.backend.infrastructure.persistence.repository;

import com.ziyara.backend.domain.enums.CashCollectionStatus;
import com.ziyara.backend.infrastructure.persistence.entity.CashCollectionJpaEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface CashCollectionJpaRepository extends JpaRepository<CashCollectionJpaEntity, UUID> {

    Optional<CashCollectionJpaEntity> findByReceiptNumber(String receiptNumber);

    List<CashCollectionJpaEntity> findByPaymentId(UUID paymentId);

    Page<CashCollectionJpaEntity> findByProviderId(UUID providerId, Pageable pageable);

    Page<CashCollectionJpaEntity> findByStatus(CashCollectionStatus status, Pageable pageable);

    List<CashCollectionJpaEntity> findByProviderIdAndStatus(UUID providerId, CashCollectionStatus status);

    @Query("""
            SELECT COALESCE(SUM(c.amount), 0)
              FROM CashCollectionJpaEntity c
             WHERE c.providerId = :providerId
               AND c.status = com.ziyara.backend.domain.enums.CashCollectionStatus.OPEN
           """)
    BigDecimal sumOpenForProvider(@Param("providerId") UUID providerId);

    @Query("""
            SELECT c
              FROM CashCollectionJpaEntity c
             WHERE c.providerId = :providerId
               AND c.collectedAt >= :from
               AND c.collectedAt <  :to
             ORDER BY c.collectedAt ASC
           """)
    List<CashCollectionJpaEntity> findByProviderIdInRange(
            @Param("providerId") UUID providerId,
            @Param("from") LocalDateTime from,
            @Param("to") LocalDateTime to);

    @Query(value = "SELECT nextval('pay_cash_receipt_seq')", nativeQuery = true)
    long nextReceiptSequence();
}
