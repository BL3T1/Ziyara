package com.ziyara.backend.application.service;

import com.ziyara.backend.application.dto.response.ProviderMediaSubmissionResponse;
import com.ziyara.backend.domain.entity.ProviderMediaSubmission;
import com.ziyara.backend.domain.repository.ProviderMediaSubmissionRepository;
import com.ziyara.backend.domain.repository.ServiceImageRepository;
import com.ziyara.backend.domain.repository.ServiceProviderRepository;
import com.ziyara.backend.infrastructure.media.MediaStorageService;
import com.ziyara.backend.infrastructure.messaging.StaffNotificationCommandPublisher;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProviderMediaSubmissionServiceTest {

    @Mock ProviderMediaSubmissionRepository submissionRepository;
    @Mock ServiceProviderRepository serviceProviderRepository;
    @Mock ServiceImageRepository serviceImageRepository;
    @Mock MediaStorageService mediaStorageService;
    @Mock StaffNotificationCommandPublisher notificationPublisher;
    @Mock AuditLogService auditLogService;

    ProviderMediaSubmissionService service;

    UUID reviewerId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        service = new ProviderMediaSubmissionService(
                submissionRepository, serviceProviderRepository, serviceImageRepository,
                mediaStorageService, notificationPublisher, auditLogService);
    }

    // ── getPendingSubmissions ─────────────────────────────────────────────────

    @Test
    void getPendingSubmissions_returnsPendingOnly() {
        ProviderMediaSubmission pending = submission(UUID.randomUUID(), "PENDING");
        when(submissionRepository.findByStatus("PENDING")).thenReturn(List.of(pending));

        List<ProviderMediaSubmissionResponse> result = service.getPendingSubmissions();

        assertThat(result).hasSize(1);
    }

    // ── approve ───────────────────────────────────────────────────────────────

    @Test
    void approve_notFound_throwsIllegalArgument() {
        UUID id = UUID.randomUUID();
        when(submissionRepository.findById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.approve(id, reviewerId))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Submission not found");
    }

    @Test
    void approve_notPending_throwsIllegalArgument() {
        UUID id = UUID.randomUUID();
        ProviderMediaSubmission sub = submission(id, "APPROVED");
        when(submissionRepository.findById(id)).thenReturn(Optional.of(sub));

        assertThatThrownBy(() -> service.approve(id, reviewerId))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("PENDING");
    }

    @Test
    void approve_pending_setsApprovedStatusAndAudits() {
        UUID id = UUID.randomUUID();
        ProviderMediaSubmission sub = submission(id, "PENDING");
        sub.setImageType("SERVICE");
        when(submissionRepository.findById(id)).thenReturn(Optional.of(sub));
        when(submissionRepository.save(any())).thenReturn(sub);

        service.approve(id, reviewerId);

        assertThat(sub.getStatus()).isEqualTo("APPROVED");
        assertThat(sub.getReviewedBy()).isEqualTo(reviewerId);
        assertThat(sub.getReviewedAt()).isNotNull();
        verify(auditLogService).logAction(any(), any(), any(), eq(reviewerId), any(), any(), any(), any());
    }

    @Test
    void approve_logoPending_updatesProviderLogoUrl() {
        UUID id = UUID.randomUUID();
        UUID providerId = UUID.randomUUID();
        ProviderMediaSubmission sub = submission(id, "PENDING");
        sub.setImageType("LOGO");
        sub.setProviderId(providerId);
        sub.setFileUrl("https://cdn.example.com/logo.jpg");

        com.ziyara.backend.domain.entity.ServiceProvider provider = new com.ziyara.backend.domain.entity.ServiceProvider();
        provider.setId(providerId);

        when(submissionRepository.findById(id)).thenReturn(Optional.of(sub));
        when(serviceProviderRepository.findById(providerId)).thenReturn(Optional.of(provider));
        when(submissionRepository.save(any())).thenReturn(sub);

        service.approve(id, reviewerId);

        assertThat(provider.getLogoUrl()).isEqualTo("https://cdn.example.com/logo.jpg");
        verify(serviceProviderRepository).save(provider);
    }

    // ── reject ────────────────────────────────────────────────────────────────

    @Test
    void reject_notPending_throwsIllegalArgument() {
        UUID id = UUID.randomUUID();
        ProviderMediaSubmission sub = submission(id, "REJECTED");
        when(submissionRepository.findById(id)).thenReturn(Optional.of(sub));

        assertThatThrownBy(() -> service.reject(id, reviewerId, "already rejected"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void reject_pending_setsRejectedStatusWithNote() {
        UUID id = UUID.randomUUID();
        ProviderMediaSubmission sub = submission(id, "PENDING");
        when(submissionRepository.findById(id)).thenReturn(Optional.of(sub));
        when(submissionRepository.save(any())).thenReturn(sub);

        service.reject(id, reviewerId, "Blurry image");

        assertThat(sub.getStatus()).isEqualTo("REJECTED");
        assertThat(sub.getReviewNote()).isEqualTo("Blurry image");
        verify(auditLogService).logAction(any(), any(), any(), eq(reviewerId), any(), any(), any(), any());
    }

    private ProviderMediaSubmission submission(UUID id, String status) {
        ProviderMediaSubmission s = new ProviderMediaSubmission();
        s.setId(id);
        s.setProviderId(UUID.randomUUID());
        s.setStatus(status);
        s.setImageType("SERVICE");
        s.setFileUrl("https://cdn.example.com/image.jpg");
        return s;
    }
}
