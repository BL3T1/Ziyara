package com.ziyara.backend.application.service;

import com.ziyara.backend.application.dto.request.CreatePortalSupportRequest;
import com.ziyara.backend.application.dto.response.PortalSupportRequestResponse;
import com.ziyara.backend.domain.entity.PortalSupportRequest;
import com.ziyara.backend.domain.enums.NotificationType;
import com.ziyara.backend.domain.repository.PortalSupportRequestRepository;
import com.ziyara.backend.domain.repository.ServiceProviderRepository;
import com.ziyara.backend.infrastructure.messaging.StaffNotificationCommandPublisher;
import com.ziyara.backend.infrastructure.messaging.StaffNotificationEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PortalSupportRequestService {

    private final PortalSupportRequestRepository repository;
    private final ServiceProviderRepository serviceProviderRepository;
    private final StaffNotificationCommandPublisher staffNotificationCommandPublisher;

    @Transactional(readOnly = true)
    public List<PortalSupportRequestResponse> listForProvider(UUID providerId) {
        return repository.findByProviderId(providerId).stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<PortalSupportRequestResponse> listAll() {
        List<PortalSupportRequest> requests = repository.findAllOrderedByCreatedAtDesc();
        Map<UUID, String> providerNames = requests.stream()
                .filter(r -> r.getProviderId() != null)
                .map(PortalSupportRequest::getProviderId)
                .distinct()
                .collect(Collectors.toMap(
                        id -> id,
                        id -> serviceProviderRepository.findById(id)
                                .map(p -> p.getName())
                                .orElse(null),
                        (a, b) -> a
                ));
        return requests.stream()
                .map(r -> toResponseWithProviderName(r, providerNames.get(r.getProviderId())))
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
        PortalSupportRequestResponse saved = toResponse(repository.save(req));

        staffNotificationCommandPublisher.publishAfterCommit(StaffNotificationEvent.builder()
                .eventId(UUID.randomUUID())
                .notificationType(NotificationType.PORTAL_SUPPORT_REQUEST.name())
                .title("Provider support request")
                .message("Provider submitted a support request: \"" + req.getSubject() + "\"")
                .notifyRoles(List.of(
                        "SUPER_ADMIN", "CEO",
                        "SUPPORT_MANAGER", "SUPPORT_AGENT",
                        "SALES_MANAGER", "SALES_REPRESENTATIVE"))
                .metadata("{\"providerId\":\"" + providerId + "\"}")
                .build());

        return saved;
    }

    @Transactional
    public PortalSupportRequestResponse respond(UUID requestId, String responseText, UUID respondedByUserId) {
        PortalSupportRequest existing = repository.findById(requestId)
                .orElseThrow(() -> new jakarta.persistence.EntityNotFoundException(
                        "Support request not found: " + requestId));
        Instant now = Instant.now();
        repository.respondToRequest(requestId, responseText.trim(), now, respondedByUserId);
        existing.setStaffResponse(responseText.trim());
        existing.setRespondedAt(now);
        existing.setRespondedByUserId(respondedByUserId);
        String providerName = existing.getProviderId() != null
                ? serviceProviderRepository.findById(existing.getProviderId())
                        .map(p -> p.getName()).orElse(null)
                : null;
        return toResponseWithProviderName(existing, providerName);
    }

    private PortalSupportRequestResponse toResponse(PortalSupportRequest r) {
        return toResponseWithProviderName(r, null);
    }

    private PortalSupportRequestResponse toResponseWithProviderName(PortalSupportRequest r, String providerName) {
        return PortalSupportRequestResponse.builder()
                .id(r.getId())
                .providerId(r.getProviderId())
                .providerName(providerName)
                .subject(r.getSubject())
                .body(r.getBody())
                .userId(r.getUserId())
                .createdAt(r.getCreatedAt())
                .staffResponse(r.getStaffResponse())
                .respondedAt(r.getRespondedAt())
                .respondedByUserId(r.getRespondedByUserId())
                .build();
    }
}
