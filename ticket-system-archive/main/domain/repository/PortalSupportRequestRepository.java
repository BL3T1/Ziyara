package com.ziyara.backend.domain.repository;

import com.ziyara.backend.domain.entity.PortalSupportRequest;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PortalSupportRequestRepository {

    PortalSupportRequest save(PortalSupportRequest request);

    Optional<PortalSupportRequest> findById(UUID id);

    List<PortalSupportRequest> findByProviderId(UUID providerId);

    List<PortalSupportRequest> findAllOrderedByCreatedAtDesc();

    void deleteById(UUID id);

    int respondToRequest(UUID id, String staffResponse, Instant respondedAt, UUID respondedByUserId);
}
