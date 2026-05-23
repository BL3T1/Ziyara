package com.ziyara.backend.application.service;

import com.ziyara.backend.application.dto.request.CreatePortalSupportRequest;
import com.ziyara.backend.application.dto.response.PortalSupportRequestResponse;
import com.ziyara.backend.domain.entity.PortalSupportRequest;
import com.ziyara.backend.domain.repository.PortalSupportRequestRepository;
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

    private final PortalSupportRequestRepository repository;

    @Transactional(readOnly = true)
    public List<PortalSupportRequestResponse> listForProvider(UUID providerId) {
        return repository.findByProviderId(providerId).stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    @Transactional
    public PortalSupportRequestResponse create(UUID providerId, UUID userId, CreatePortalSupportRequest request) {
        PortalSupportRequest req = new PortalSupportRequest();
        req.setProviderId(providerId);
        req.setUserId(userId);
        req.setSubject(request.getSubject().trim());
        req.setBody(request.getBody().trim());
        req.setCreatedAt(Instant.now());
        return toResponse(repository.save(req));
    }

    private PortalSupportRequestResponse toResponse(PortalSupportRequest r) {
        return PortalSupportRequestResponse.builder()
                .id(r.getId())
                .subject(r.getSubject())
                .body(r.getBody())
                .userId(r.getUserId())
                .createdAt(r.getCreatedAt())
                .build();
    }
}
