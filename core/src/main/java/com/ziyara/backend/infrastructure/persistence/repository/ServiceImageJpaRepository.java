package com.ziyara.backend.infrastructure.persistence.repository;

import com.ziyara.backend.infrastructure.persistence.entity.ServiceImageJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Spring Data JPA Repository: ServiceImageJpaRepository
 */
@Repository
public interface ServiceImageJpaRepository extends JpaRepository<ServiceImageJpaEntity, UUID> {
    List<ServiceImageJpaEntity> findByServiceIdOrderByDisplayOrderAscIdAsc(UUID serviceId);

    List<ServiceImageJpaEntity> findByServiceId(UUID serviceId);

    Optional<ServiceImageJpaEntity> findByServiceIdAndIsPrimary(UUID serviceId, Boolean isPrimary);

    long countByServiceId(UUID serviceId);

    @Modifying
    @Query("UPDATE ServiceImageJpaEntity s SET s.isPrimary = false WHERE s.serviceId = :serviceId")
    int clearPrimaryByServiceId(@Param("serviceId") UUID serviceId);

    @Modifying
    @Query("UPDATE ServiceImageJpaEntity s SET s.isPrimary = false WHERE s.serviceId = :serviceId AND s.id <> :excludeId")
    int clearPrimaryByServiceIdExcept(@Param("serviceId") UUID serviceId, @Param("excludeId") UUID excludeId);
}
