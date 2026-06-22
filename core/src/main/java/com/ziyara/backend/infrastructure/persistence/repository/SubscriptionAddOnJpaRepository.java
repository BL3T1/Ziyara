package com.ziyara.backend.infrastructure.persistence.repository;

import com.ziyara.backend.infrastructure.persistence.entity.SubscriptionAddOnJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Repository
public interface SubscriptionAddOnJpaRepository
        extends JpaRepository<SubscriptionAddOnJpaEntity, UUID> {

    List<SubscriptionAddOnJpaEntity> findBySubscriptionId(UUID subscriptionId);

    /** Active add-ons: status = ACTIVE and either no expiry or expiry in the future. */
    @Query("SELECT a FROM SubscriptionAddOnJpaEntity a "
         + "WHERE a.subscriptionId = :subscriptionId "
         + "  AND a.status = 'ACTIVE' "
         + "  AND (a.expiresAt IS NULL OR a.expiresAt > :now)")
    List<SubscriptionAddOnJpaEntity> findActiveBySubscriptionId(
            @Param("subscriptionId") UUID subscriptionId,
            @Param("now") Instant now);
}
