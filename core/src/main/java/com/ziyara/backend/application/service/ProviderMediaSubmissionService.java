package com.ziyara.backend.application.service;

import com.ziyara.backend.application.dto.response.ProviderMediaSubmissionResponse;
import com.ziyara.backend.domain.entity.ProviderMediaSubmission;
import com.ziyara.backend.domain.entity.ServiceImage;
import com.ziyara.backend.domain.entity.ServiceProvider;
import com.ziyara.backend.domain.enums.NotificationType;
import com.ziyara.backend.domain.enums.ServiceImageCategory;
import com.ziyara.backend.domain.repository.ProviderMediaSubmissionRepository;
import com.ziyara.backend.domain.repository.ServiceImageRepository;
import com.ziyara.backend.domain.repository.ServiceProviderRepository;
import com.ziyara.backend.infrastructure.media.MediaStorageService;
import com.ziyara.backend.infrastructure.messaging.StaffNotificationCommandPublisher;
import com.ziyara.backend.infrastructure.messaging.StaffNotificationEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class ProviderMediaSubmissionService {

    private final ProviderMediaSubmissionRepository submissionRepository;
    private final ServiceProviderRepository serviceProviderRepository;
    private final ServiceImageRepository serviceImageRepository;
    private final MediaStorageService mediaStorageService;
    private final StaffNotificationCommandPublisher notificationPublisher;
    private final AuditLogService auditLogService;

    @Transactional
    public ProviderMediaSubmissionResponse submitServiceImage(
            UUID providerId, UUID serviceId, byte[] bytes, String contentType, String filename,
            String imageType, String altText, boolean primary, UUID submittedBy) {
        String url = mediaStorageService.storeServiceImage(serviceId, bytes, contentType, filename);
        ProviderMediaSubmission sub = new ProviderMediaSubmission();
        sub.setProviderId(providerId);
        sub.setServiceId(serviceId);
        sub.setImageType(imageType != null ? imageType.toUpperCase() : "SERVICE");
        sub.setFileUrl(url);
        sub.setAltText(altText);
        sub.setPrimary(primary);
        sub.setSubmittedBy(submittedBy);
        ProviderMediaSubmission saved = submissionRepository.save(sub);
        sendAdminNotification(saved, providerId);
        log.info("Media submission created {} for provider {} service {}", saved.getId(), providerId, serviceId);
        return toResponse(saved);
    }

    @Transactional
    public ProviderMediaSubmissionResponse submitProviderLogo(
            UUID providerId, byte[] bytes, String contentType, String filename, String altText, UUID submittedBy) {
        String url = mediaStorageService.storeProviderImage(providerId, bytes, contentType, filename);
        ProviderMediaSubmission sub = new ProviderMediaSubmission();
        sub.setProviderId(providerId);
        sub.setImageType("LOGO");
        sub.setContextKey("logo");
        sub.setFileUrl(url);
        sub.setAltText(altText);
        sub.setPrimary(true);
        sub.setSubmittedBy(submittedBy);
        ProviderMediaSubmission saved = submissionRepository.save(sub);
        sendAdminNotification(saved, providerId);
        log.info("Logo submission created {} for provider {}", saved.getId(), providerId);
        return toResponse(saved);
    }

    @Transactional(readOnly = true)
    public List<ProviderMediaSubmissionResponse> getProviderSubmissions(UUID providerId) {
        return submissionRepository.findByProviderId(providerId).stream()
                .map(this::toResponse).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<ProviderMediaSubmissionResponse> getPendingSubmissions() {
        return submissionRepository.findByStatus("PENDING").stream()
                .map(this::toResponse).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<ProviderMediaSubmissionResponse> getAllSubmissions() {
        return submissionRepository.findByStatus("PENDING").stream()
                .map(this::toResponse).collect(Collectors.toList());
    }

    @Transactional
    public ProviderMediaSubmissionResponse approve(UUID submissionId, UUID reviewerId) {
        ProviderMediaSubmission sub = submissionRepository.findById(submissionId)
                .orElseThrow(() -> new IllegalArgumentException("Submission not found: " + submissionId));
        if (!"PENDING".equals(sub.getStatus())) {
            throw new IllegalArgumentException("Only PENDING submissions can be approved");
        }
        sub.setStatus("APPROVED");
        sub.setReviewedBy(reviewerId);
        sub.setReviewedAt(LocalDateTime.now());

        if ("LOGO".equals(sub.getImageType())) {
            serviceProviderRepository.findById(sub.getProviderId()).ifPresent(provider -> {
                provider.setLogoUrl(sub.getFileUrl());
                serviceProviderRepository.save(provider);
            });
        } else if (sub.getServiceId() != null) {
            ServiceImage img = new ServiceImage();
            img.setServiceId(sub.getServiceId());
            img.setUrl(sub.getFileUrl());
            img.setAltText(sub.getAltText());
            img.setPrimary(sub.isPrimary());
            img.setDisplayOrder(0);
            img.setCategory(ServiceImageCategory.OTHER);
            serviceImageRepository.save(img);
        }

        ProviderMediaSubmission saved = submissionRepository.save(sub);
        auditLogService.logAction("MEDIA_SUBMISSION_APPROVE", "ProviderMediaSubmission",
                submissionId.toString(), reviewerId, "PENDING", "APPROVED", null, null);
        log.info("Media submission {} approved by {}", submissionId, reviewerId);
        return toResponse(saved);
    }

    @Transactional
    public ProviderMediaSubmissionResponse reject(UUID submissionId, UUID reviewerId, String note) {
        ProviderMediaSubmission sub = submissionRepository.findById(submissionId)
                .orElseThrow(() -> new IllegalArgumentException("Submission not found: " + submissionId));
        if (!"PENDING".equals(sub.getStatus())) {
            throw new IllegalArgumentException("Only PENDING submissions can be rejected");
        }
        sub.setStatus("REJECTED");
        sub.setReviewedBy(reviewerId);
        sub.setReviewedAt(LocalDateTime.now());
        sub.setReviewNote(note);
        ProviderMediaSubmission saved = submissionRepository.save(sub);
        auditLogService.logAction("MEDIA_SUBMISSION_REJECT", "ProviderMediaSubmission",
                submissionId.toString(), reviewerId, "PENDING", "REJECTED:" + (note != null ? note : ""), null, null);
        log.info("Media submission {} rejected by {}", submissionId, reviewerId);
        return toResponse(saved);
    }

    private void sendAdminNotification(ProviderMediaSubmission sub, UUID providerId) {
        ServiceProvider provider = serviceProviderRepository.findById(providerId).orElse(null);
        String providerName = provider != null ? provider.getName() : providerId.toString();
        notificationPublisher.publishAfterCommit(StaffNotificationEvent.builder()
                .eventId(UUID.randomUUID())
                .notificationType(NotificationType.MEDIA_SUBMISSION_PENDING.name())
                .title("New media submission pending review")
                .message("Partner \"" + providerName + "\" submitted a " + sub.getImageType().toLowerCase() + " image for approval.")
                .notifyRoles(List.of("SUPER_ADMIN", "CEO", "SALES_MANAGER", "OPERATIONS_MANAGER"))
                .metadata("{\"submissionId\":\"" + sub.getId() + "\",\"providerId\":\"" + providerId + "\"}")
                .build());
    }

    private ProviderMediaSubmissionResponse toResponse(ProviderMediaSubmission s) {
        return ProviderMediaSubmissionResponse.builder()
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
                .build();
    }
}
