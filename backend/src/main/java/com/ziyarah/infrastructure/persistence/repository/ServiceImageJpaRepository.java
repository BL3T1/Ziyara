package com.ziyarah.infrastructure.persistence.repository;

import com.ziyarah.infrastructure.persistence.entity.ServiceImageJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Spring Data JPA Repository: ServiceImageJpaRepository
 */
@Repository
public interface ServiceImageJpaRepository extends JpaRepository<ServiceImageJpaEntity, UUID> {
    List<ServiceImageJpaEntity> findByServiceId(UUID serviceId);
    Optional<ServiceImageJpaEntity> findByServiceIdAndIsPrimary(UUID serviceId, Boolean isPrimary);
}
