package com.ziyara.backend.infrastructure.persistence.adapter;

import com.ziyara.backend.domain.entity.ProviderMediaSubmission;
import com.ziyara.backend.domain.repository.ProviderMediaSubmissionRepository;
import com.ziyara.backend.infrastructure.persistence.entity.ProviderMediaSubmissionJpaEntity;
import com.ziyara.backend.infrastructure.persistence.repository.ProviderMediaSubmissionJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class ProviderMediaSubmissionRepositoryAdapter implements ProviderMediaSubmissionRepository {

    private final ProviderMediaSubmissionJpaRepository jpaRepository;

    @Override
    public ProviderMediaSubmission save(ProviderMediaSubmission submission) {
        return toDomain(jpaRepository.save(toJpa(submission)));
    }

    @Override
    public Optional<ProviderMediaSubmission> findById(UUID id) {
        return jpaRepository.findById(id).map(this::toDomain);
    }

    @Override
    public List<ProviderMediaSubmission> findByProviderId(UUID providerId) {
        return jpaRepository.findByProviderId(providerId).stream().map(this::toDomain).collect(Collectors.toList());
    }

    @Override
    public List<ProviderMediaSubmission> findByStatus(String status) {
        return jpaRepository.findByStatusOrderBySubmittedAtDesc(status).stream().map(this::toDomain).collect(Collectors.toList());
    }

    @Override
    public List<ProviderMediaSubmission> findByServiceId(UUID serviceId) {
        return jpaRepository.findByServiceId(serviceId).stream().map(this::toDomain).collect(Collectors.toList());
    }

    private ProviderMediaSubmission toDomain(ProviderMediaSubmissionJpaEntity e) {
        ProviderMediaSubmission s = new ProviderMediaSubmission();
        s.setId(e.getId());
        s.setProviderId(e.getProviderId());
        s.setServiceId(e.getServiceId());
        s.setImageType(e.getImageType());
        s.setContextKey(e.getContextKey());
        s.setFileUrl(e.getFileUrl());
        s.setAltText(e.getAltText());
        s.setPrimary(Boolean.TRUE.equals(e.getPrimary()));
        s.setStatus(e.getStatus());
        s.setSubmittedBy(e.getSubmittedBy());
        s.setSubmittedAt(e.getSubmittedAt());
        s.setReviewedBy(e.getReviewedBy());
        s.setReviewedAt(e.getReviewedAt());
        s.setReviewNote(e.getReviewNote());
        s.setCreatedAt(e.getCreatedAt());
        s.setUpdatedAt(e.getUpdatedAt());
        return s;
    }

    private ProviderMediaSubmissionJpaEntity toJpa(ProviderMediaSubmission s) {
        return ProviderMediaSubmissionJpaEntity.builder()
                .id(s.getId())
                .providerId(s.getProviderId())
                .serviceId(s.getServiceId())
                .imageType(s.getImageType())
                .contextKey(s.getContextKey())
                .fileUrl(s.getFileUrl())
                .altText(s.getAltText())
                .primary(s.isPrimary())
                .status(s.getStatus())
                .submittedBy(s.getSubmittedBy())
                .submittedAt(s.getSubmittedAt())
                .reviewedBy(s.getReviewedBy())
                .reviewedAt(s.getReviewedAt())
                .reviewNote(s.getReviewNote())
                .createdAt(s.getCreatedAt())
                .updatedAt(s.getUpdatedAt())
                .build();
    }
}
