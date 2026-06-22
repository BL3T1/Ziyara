package com.ziyara.backend.infrastructure.persistence.repository;

import com.ziyara.backend.domain.enums.ProviderStatus;
import com.ziyara.backend.infrastructure.persistence.entity.ServiceProviderJpaEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.time.LocalDate;
import java.time.LocalDateTime;
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

    List<ServiceProviderJpaEntity> findByProviderType(String providerType);

    Page<ServiceProviderJpaEntity> findByProviderType(String providerType, Pageable pageable);

    Page<ServiceProviderJpaEntity> findByStatusAndProviderType(ProviderStatus status, String providerType, Pageable pageable);
    boolean existsByCompanyName(String companyName);

    @Query("SELECT e FROM ServiceProviderJpaEntity e WHERE e.expiryDate = :date AND e.deletedAt IS NULL")
    List<ServiceProviderJpaEntity> findByExpiryDateAndNotDeleted(@Param("date") LocalDate date);

    @Modifying
    @Query("UPDATE ServiceProviderJpaEntity e SET e.deletedAt = :deletedAt WHERE e.id = :id")
    void softDeleteById(@Param("id") UUID id, @Param("deletedAt") LocalDateTime deletedAt);
}
