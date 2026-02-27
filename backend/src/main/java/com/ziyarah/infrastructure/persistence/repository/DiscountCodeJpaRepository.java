package com.ziyarah.infrastructure.persistence.repository;

import com.ziyarah.domain.enums.DiscountStatus;
import com.ziyarah.infrastructure.persistence.entity.DiscountCodeJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
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
}
