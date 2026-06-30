package com.ziyara.backend.infrastructure.persistence.repository;

import com.ziyara.backend.domain.enums.DiscountStatus;
import com.ziyara.backend.infrastructure.persistence.entity.DiscountCodeJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Spring Data JPA Repository: DiscountCodeJpaRepository
 */
@Repository
public interface DiscountCodeJpaRepository extends JpaRepository<DiscountCodeJpaEntity, UUID> {
    Optional<DiscountCodeJpaEntity> findByCode(String code);
    List<DiscountCodeJpaEntity> findByStatus(DiscountStatus status);
    boolean existsByCode(String code);

    @Query(value = "SELECT * FROM disc_discount_codes WHERE provider_id = :providerId ORDER BY created_at DESC LIMIT :limit OFFSET :offset", nativeQuery = true)
    List<DiscountCodeJpaEntity> findByProviderIdPaged(@Param("providerId") UUID providerId,
                                                       @Param("limit") int limit,
                                                       @Param("offset") long offset);

    @Query("SELECT COUNT(d) FROM DiscountCodeJpaEntity d WHERE d.providerId = :providerId")
    long countByProviderId(@Param("providerId") UUID providerId);

    boolean existsByIdAndProviderId(UUID id, UUID providerId);
}
