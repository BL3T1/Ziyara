package com.ziyara.backend.application.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ziyara.backend.application.dto.response.DataExportRequestResponse;
import com.ziyara.backend.domain.entity.DataExportRequest;
import com.ziyara.backend.domain.repository.BookingRepository;
import com.ziyara.backend.domain.repository.DataExportRequestRepository;
import com.ziyara.backend.domain.repository.NotificationRepository;
import com.ziyara.backend.domain.repository.UserConsentRepository;
import com.ziyara.backend.domain.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DataExportServiceTest {

    @Mock DataExportRequestRepository exportRepository;
    @Mock UserRepository userRepository;
    @Mock BookingRepository bookingRepository;
    @Mock UserConsentRepository userConsentRepository;
    @Mock NotificationRepository notificationRepository;
    @Mock ApplicationEventPublisher eventPublisher;

    DataExportService service;

    UUID userId = UUID.randomUUID();
    UUID exportId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        service = new DataExportService(
                exportRepository, userRepository, bookingRepository,
                userConsentRepository, notificationRepository,
                new ObjectMapper(), eventPublisher);
    }

    // ── list ──────────────────────────────────────────────────────────────────

    @Test
    void list_returnsMappedResponses() {
        DataExportRequest row = export(exportId, userId, "PENDING");
        when(exportRepository.findByUserIdOrderedDesc(userId)).thenReturn(List.of(row));

        List<DataExportRequestResponse> result = service.list(userId);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getId()).isEqualTo(exportId);
        assertThat(result.get(0).getStatus()).isEqualTo("PENDING");
    }

    @Test
    void list_noExports_returnsEmpty() {
        when(exportRepository.findByUserIdOrderedDesc(userId)).thenReturn(List.of());

        assertThat(service.list(userId)).isEmpty();
    }

    // ── getForUser ─────────────────────────────────────────────────────────────

    @Test
    void getForUser_exportNotFound_throwsIllegalArgument() {
        when(exportRepository.findById(exportId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getForUser(exportId, userId))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Export not found");
    }

    @Test
    void getForUser_differentUser_throwsIllegalArgument() {
        DataExportRequest row = export(exportId, UUID.randomUUID(), "PENDING");
        when(exportRepository.findById(exportId)).thenReturn(Optional.of(row));

        assertThatThrownBy(() -> service.getForUser(exportId, userId))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Export not found");
    }

    @Test
    void getForUser_matchingUser_returnsResponse() {
        DataExportRequest row = export(exportId, userId, "COMPLETED");
        when(exportRepository.findById(exportId)).thenReturn(Optional.of(row));

        DataExportRequestResponse result = service.getForUser(exportId, userId);

        assertThat(result.getId()).isEqualTo(exportId);
        assertThat(result.getStatus()).isEqualTo("COMPLETED");
    }

    // ── downloadPayloadForUser ────────────────────────────────────────────────

    @Test
    void downloadPayloadForUser_notCompleted_throwsIllegalState() {
        DataExportRequest row = export(exportId, userId, "PENDING");
        when(exportRepository.findById(exportId)).thenReturn(Optional.of(row));

        assertThatThrownBy(() -> service.downloadPayloadForUser(exportId, userId))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("not ready");
    }

    @Test
    void downloadPayloadForUser_completedNullPayload_throwsIllegalState() {
        DataExportRequest row = export(exportId, userId, "COMPLETED");
        row.setPayloadJson(null);
        when(exportRepository.findById(exportId)).thenReturn(Optional.of(row));

        assertThatThrownBy(() -> service.downloadPayloadForUser(exportId, userId))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void downloadPayloadForUser_completedWithPayload_returnsBytes() {
        DataExportRequest row = export(exportId, userId, "COMPLETED");
        row.setPayloadJson("{\"userId\":\"abc\"}");
        when(exportRepository.findById(exportId)).thenReturn(Optional.of(row));

        byte[] bytes = service.downloadPayloadForUser(exportId, userId);

        assertThat(new String(bytes, StandardCharsets.UTF_8)).contains("userId");
    }

    // ── requestExport ─────────────────────────────────────────────────────────

    @Test
    void requestExport_userNotFound_throwsIllegalArgument() {
        when(userRepository.existsById(userId)).thenReturn(false);

        assertThatThrownBy(() -> service.requestExport(userId))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("User not found");
    }

    @Test
    void requestExport_userExists_savesPendingAndPublishesEvent() {
        when(userRepository.existsById(userId)).thenReturn(true);
        DataExportRequest saved = export(exportId, userId, "PENDING");
        when(exportRepository.save(any())).thenReturn(saved);

        DataExportRequestResponse result = service.requestExport(userId);

        ArgumentCaptor<DataExportRequest> captor = ArgumentCaptor.forClass(DataExportRequest.class);
        verify(exportRepository).save(captor.capture());
        assertThat(captor.getValue().getStatus()).isEqualTo("PENDING");
        assertThat(captor.getValue().getFormat()).isEqualTo("JSON");
        verify(eventPublisher).publishEvent(any());
        assertThat(result.getStatus()).isEqualTo("PENDING");
    }

    private DataExportRequest export(UUID id, UUID userId, String status) {
        DataExportRequest r = new DataExportRequest();
        r.setId(id);
        r.setUserId(userId);
        r.setStatus(status);
        r.setFormat("JSON");
        r.setRequestedAt(LocalDateTime.now());
        return r;
    }
}
