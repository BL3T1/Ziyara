package com.ziyara.backend.infrastructure.persistence.repository;

import com.ziyara.backend.infrastructure.persistence.entity.PortalPayoutRequestJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Repository
public interface PortalPayoutRequestJpaRepository extends JpaRepository<PortalPayoutRequestJpaEntity, UUID> {

    @Query("SELECT COALESCE(SUM(p.amount), 0) FROM PortalPayoutRequestJpaEntity p WHERE p.status = :status")
    BigDecimal sumAmountByStatus(@Param("status") String status);

    @Query("SELECT COUNT(p) FROM PortalPayoutRequestJpaEntity p WHERE p.status = :status")
    long countByStatus(@Param("status") String status);

    @Query("SELECT COUNT(p) FROM PortalPayoutRequestJpaEntity p WHERE p.status IN :statuses")
    long countByStatuses(@Param("statuses") List<String> statuses);

    @Query("SELECT COUNT(p) FROM PortalPayoutRequestJpaEntity p WHERE p.providerId = :providerId")
    long countByProviderId(@Param("providerId") UUID providerId);

    @Query(value = """
            SELECT * FROM portal_payout_requests
            WHERE provider_id = :providerId
            ORDER BY requested_at DESC
            LIMIT :limit OFFSET :offset
            """, nativeQuery = true)
    List<PortalPayoutRequestJpaEntity> findByProviderIdPaged(@Param("providerId") UUID providerId,
                                                              @Param("limit") int limit,
                                                              @Param("offset") long offset);
}
