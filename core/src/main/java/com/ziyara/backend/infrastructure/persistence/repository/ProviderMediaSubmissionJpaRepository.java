package com.ziyara.backend.infrastructure.persistence.repository;

import com.ziyara.backend.infrastructure.persistence.entity.ProviderMediaSubmissionJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.UUID;

@Repository
public interface ProviderMediaSubmissionJpaRepository extends JpaRepository<ProviderMediaSubmissionJpaEntity, UUID> {
    List<ProviderMediaSubmissionJpaEntity> findByProviderId(UUID providerId);
    List<ProviderMediaSubmissionJpaEntity> findByStatus(String status);
    List<ProviderMediaSubmissionJpaEntity> findByServiceId(UUID serviceId);
    List<ProviderMediaSubmissionJpaEntity> findByStatusOrderBySubmittedAtDesc(String status);
}
