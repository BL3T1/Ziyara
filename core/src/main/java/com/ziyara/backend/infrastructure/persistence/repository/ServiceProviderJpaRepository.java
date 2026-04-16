package com.ziyara.backend.infrastructure.persistence.repository;

import com.ziyara.backend.domain.enums.ProviderStatus;
import com.ziyara.backend.infrastructure.persistence.entity.ServiceProviderJpaEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Spring Data JPA Repository: ServiceProviderJpaRepository
 */
@Repository
public interface ServiceProviderJpaRepository extends JpaRepository<ServiceProviderJpaEntity, UUID> {
    Optional<ServiceProviderJpaEntity> findByCreatedBy(UUID createdBy);
    Optional<ServiceProviderJpaEntity> findByCompanyName(String companyName);
    List<ServiceProviderJpaEntity> findByStatus(ProviderStatus status);

    Page<ServiceProviderJpaEntity> findByStatus(ProviderStatus status, Pageable pageable);
    @Query("SELECT s FROM ServiceProviderJpaEntity s WHERE 1 = 0")
    List<ServiceProviderJpaEntity> findByType(String type);
    boolean existsByCompanyName(String companyName);
}
