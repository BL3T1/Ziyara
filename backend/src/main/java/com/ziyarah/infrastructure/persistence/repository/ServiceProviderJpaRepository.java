package com.ziyarah.infrastructure.persistence.repository;

import com.ziyarah.domain.enums.ProviderStatus;
import com.ziyarah.infrastructure.persistence.entity.ServiceProviderJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Spring Data JPA Repository: ServiceProviderJpaRepository
 */
@Repository
public interface ServiceProviderJpaRepository extends JpaRepository<ServiceProviderJpaEntity, UUID> {
    Optional<ServiceProviderJpaEntity> findByUserId(UUID userId);
    Optional<ServiceProviderJpaEntity> findByName(String name);
    List<ServiceProviderJpaEntity> findByStatus(ProviderStatus status);
    List<ServiceProviderJpaEntity> findByType(String type);
    boolean existsByName(String name);
}
