package com.ziyara.backend.application.service;

import com.ziyara.backend.application.dto.response.AuditLogResponse;
import com.ziyara.backend.domain.common.PagedResult;
import com.ziyara.backend.domain.entity.AuditLog;
import com.ziyara.backend.domain.repository.AuditLogRepository;
import com.ziyara.backend.domain.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuditLogServiceTest {

    @Mock AuditLogRepository auditLogRepository;
    @Mock UserRepository userRepository;

    AuditLogService service;

    @BeforeEach
    void setUp() {
        service = new AuditLogService(auditLogRepository, userRepository);
    }

    // ── getRecentLogs ─────────────────────────────────────────────────────────

    @Test
    void getRecentLogs_returnsAllLogsWhenNoSearch() {
        AuditLog log = auditLog("CREATE", "User", "u-1");
        when(auditLogRepository.findRecent(any()))
                .thenReturn(new PagedResult<>(List.of(log), 1, 0, 20));
        when(userRepository.findById(any())).thenReturn(Optional.empty());

        List<AuditLogResponse> result = service.getRecentLogs(20, null);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getAction()).isEqualTo("CREATE");
    }

    @Test
    void getRecentLogs_filtersWhenSearchTermGiven() {
        AuditLog logA = auditLog("CREATE", "User", "u-1");
        AuditLog logB = auditLog("DELETE", "Role", "r-1");
        when(auditLogRepository.findRecent(any()))
                .thenReturn(new PagedResult<>(List.of(logA, logB), 2, 0, 20));
        when(userRepository.findById(any())).thenReturn(Optional.empty());

        List<AuditLogResponse> result = service.getRecentLogs(20, "create");

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getAction()).isEqualTo("CREATE");
    }

    @Test
    void getRecentLogs_emptyRepository_returnsEmpty() {
        when(auditLogRepository.findRecent(any()))
                .thenReturn(new PagedResult<>(List.of(), 0, 0, 20));

        List<AuditLogResponse> result = service.getRecentLogs(10, null);

        assertThat(result).isEmpty();
    }

    // ── logAction ─────────────────────────────────────────────────────────────

    @Test
    void logAction_nullEntityName_usesSystemAsEntityType() {
        service.logAction("READ", null, null, null, null, null, null, null);

        org.mockito.ArgumentCaptor<AuditLog> captor = org.mockito.ArgumentCaptor.forClass(AuditLog.class);
        org.mockito.Mockito.verify(auditLogRepository).save(captor.capture());
        assertThat(captor.getValue().getEntityType()).isEqualTo("System");
    }

    @Test
    void logAction_withEntityName_setsEntityType() {
        service.logAction("UPDATE", "Service", "svc-1", UUID.randomUUID(), null, null, "127.0.0.1", "curl");

        org.mockito.ArgumentCaptor<AuditLog> captor = org.mockito.ArgumentCaptor.forClass(AuditLog.class);
        org.mockito.Mockito.verify(auditLogRepository).save(captor.capture());
        assertThat(captor.getValue().getEntityType()).isEqualTo("Service");
    }

    // ── getEntityLogs ─────────────────────────────────────────────────────────

    @Test
    void getEntityLogs_returnsLogsForEntity() {
        AuditLog log = auditLog("UPDATE", "Service", "svc-1");
        when(auditLogRepository.findByEntityNameAndEntityId("Service", "svc-1"))
                .thenReturn(List.of(log));
        when(userRepository.findById(any())).thenReturn(Optional.empty());

        List<AuditLogResponse> result = service.getEntityLogs("Service", "svc-1");

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getEntityName()).isEqualTo("Service");
    }

    // ── getUserLogs ───────────────────────────────────────────────────────────

    @Test
    void getUserLogs_returnsLogsForUser() {
        UUID userId = UUID.randomUUID();
        AuditLog log = auditLog("LOGIN", "Auth", null);
        log.setUserId(userId);
        when(auditLogRepository.findByUserId(userId)).thenReturn(List.of(log));
        when(userRepository.findById(userId)).thenReturn(Optional.empty());

        List<AuditLogResponse> result = service.getUserLogs(userId);

        assertThat(result).hasSize(1);
    }

    // ── getFilteredLogs ───────────────────────────────────────────────────────

    @Test
    void getFilteredLogs_withNullDates_usesDefaultBounds() {
        when(auditLogRepository.findFiltered(any(), any(), any(), any(LocalDateTime.class), any(LocalDateTime.class), any()))
                .thenReturn(new PagedResult<>(List.of(), 0, 0, 20));

        org.springframework.data.domain.Page<AuditLogResponse> result =
                service.getFilteredLogs(null, null, null, null, null, 0, 20);

        assertThat(result).isEmpty();
    }

    private AuditLog auditLog(String action, String entityName, String entityId) {
        AuditLog log = new AuditLog();
        log.setId(UUID.randomUUID());
        log.setAction(action);
        log.setEntityName(entityName);
        log.setEntityType(entityName != null ? entityName : "System");
        log.setEntityId(entityId);
        return log;
    }
}
