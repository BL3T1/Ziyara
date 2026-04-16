package com.ziyara.backend.application.service;

import com.ziyara.backend.application.dto.request.CreatePortalSupportRequest;
import com.ziyara.backend.application.dto.response.PortalSupportRequestResponse;
import com.ziyara.backend.infrastructure.persistence.entity.PortalSupportRequestJpaEntity;
import com.ziyara.backend.infrastructure.persistence.repository.PortalSupportRequestJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PortalSupportRequestService {

    private final PortalSupportRequestJpaRepository repository;

    @Transactional(readOnly = true)
    public List<PortalSupportRequestResponse> listForProvider(UUID providerId) {
        return repository.findByProviderIdOrderByCreatedAtDesc(providerId).stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    @Transactional
    public PortalSupportRequestResponse create(UUID providerId, UUID userId, CreatePortalSupportRequest request) {
        PortalSupportRequestJpaEntity row = PortalSupportRequestJpaEntity.builder()
                .providerId(providerId)
                .userId(userId)
                .subject(request.getSubject().trim())
                .body(request.getBody().trim())
                .createdAt(Instant.now())
                .build();
        return toResponse(repository.save(row));
    }

    private PortalSupportRequestResponse toResponse(PortalSupportRequestJpaEntity e) {
        return PortalSupportRequestResponse.builder()
                .id(e.getId())
                .subject(e.getSubject())
                .body(e.getBody())
                .userId(e.getUserId())
                .createdAt(e.getCreatedAt())
                .build();
    }
}
