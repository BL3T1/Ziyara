package com.ziyara.backend.domain.repository;

import com.ziyara.backend.domain.entity.ProviderMediaSubmission;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ProviderMediaSubmissionRepository {
    ProviderMediaSubmission save(ProviderMediaSubmission submission);
    Optional<ProviderMediaSubmission> findById(UUID id);
    List<ProviderMediaSubmission> findByProviderId(UUID providerId);
    List<ProviderMediaSubmission> findByStatus(String status);
    List<ProviderMediaSubmission> findByServiceId(UUID serviceId);
}
