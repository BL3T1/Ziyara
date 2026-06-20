package com.ziyara.backend.infrastructure.persistence.repository;

import com.ziyara.backend.domain.enums.SubscriptionStatus;
import com.ziyara.backend.infrastructure.persistence.entity.CustomerSubscriptionJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface CustomerSubscriptionJpaRepository
        extends JpaRepository<CustomerSubscriptionJpaEntity, UUID> {

    /** Returns the single TRIAL or ACTIVE subscription for a provider, if any. */
    @Query("SELECT s FROM CustomerSubscriptionJpaEntity s "
         + "WHERE s.providerId = :providerId "
         + "  AND s.status IN ('TRIAL', 'ACTIVE') "
         + "ORDER BY s.createdAt DESC")
    Optional<CustomerSubscriptionJpaEntity> findActiveByProviderId(@Param("providerId") UUID providerId);

    List<CustomerSubscriptionJpaEntity> findAllByProviderId(UUID providerId);

    List<CustomerSubscriptionJpaEntity> findAllByStatus(SubscriptionStatus status);
}
