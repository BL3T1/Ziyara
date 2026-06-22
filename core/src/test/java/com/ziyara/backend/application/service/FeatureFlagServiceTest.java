package com.ziyara.backend.application.service;

import com.ziyara.backend.application.dto.request.UpsertFeatureFlagRequest;
import com.ziyara.backend.application.dto.response.FeatureFlagResponse;
import com.ziyara.backend.domain.entity.FeatureFlag;
import com.ziyara.backend.domain.repository.FeatureFlagRepository;
import com.ziyara.backend.infrastructure.messaging.StaffNotificationCommandPublisher;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class FeatureFlagServiceTest {

    @Mock FeatureFlagRepository repository;
    @Mock AuditLogService auditLogService;
    @Mock StaffNotificationCommandPublisher staffNotificationCommandPublisher;

    FeatureFlagService service;

    @BeforeEach
    void setUp() {
        service = new FeatureFlagService(repository, auditLogService, staffNotificationCommandPublisher);
    }

    // ── listAll ───────────────────────────────────────────────────────────────

    @Test
    void listAll_returnsAllFlagsSortedByKey() {
        FeatureFlag f1 = flagWithKey("z-flag");
        FeatureFlag f2 = flagWithKey("a-flag");
        when(repository.findAll()).thenReturn(List.of(f1, f2));

        List<FeatureFlagResponse> result = service.listAll();

        assertThat(result).hasSize(2);
        assertThat(result.get(0).getFlagKey()).isEqualTo("a-flag");
        assertThat(result.get(1).getFlagKey()).isEqualTo("z-flag");
    }

    @Test
    void listAll_emptyRepository_returnsEmpty() {
        when(repository.findAll()).thenReturn(List.of());

        assertThat(service.listAll()).isEmpty();
    }

    // ── upsert ────────────────────────────────────────────────────────────────

    @Test
    void upsert_newFlag_createsWithEnabledFalseByDefault() {
        UUID actorId = UUID.randomUUID();
        UpsertFeatureFlagRequest request = new UpsertFeatureFlagRequest();
        request.setFlagKey("new-feature");

        when(repository.findByKey("new-feature")).thenReturn(Optional.empty());

        FeatureFlag saved = flagWithKey("new-feature");
        saved.setEnabled(false);
        when(repository.save(any())).thenReturn(saved);

        FeatureFlagResponse result = service.upsert(request, actorId);

        assertThat(result.getFlagKey()).isEqualTo("new-feature");
        assertThat(result.isEnabled()).isFalse();
        verify(auditLogService).logAction(any(), any(), any(), eq(actorId), any(), any(), any(), any());
        verify(staffNotificationCommandPublisher).publishAfterCommit(any());
    }

    @Test
    void upsert_existingFlag_updatesEnabledState() {
        UUID actorId = UUID.randomUUID();
        UpsertFeatureFlagRequest request = new UpsertFeatureFlagRequest();
        request.setFlagKey("existing-flag");
        request.setEnabled(true);

        FeatureFlag existing = flagWithKey("existing-flag");
        existing.setEnabled(false);
        when(repository.findByKey("existing-flag")).thenReturn(Optional.of(existing));

        FeatureFlag saved = flagWithKey("existing-flag");
        saved.setEnabled(true);
        when(repository.save(any())).thenReturn(saved);

        service.upsert(request, actorId);

        ArgumentCaptor<FeatureFlag> captor = ArgumentCaptor.forClass(FeatureFlag.class);
        verify(repository).save(captor.capture());
        assertThat(captor.getValue().isEnabled()).isTrue();
    }

    @Test
    void upsert_flagKeyTrimmed_savesNormalizedKey() {
        UUID actorId = UUID.randomUUID();
        UpsertFeatureFlagRequest request = new UpsertFeatureFlagRequest();
        request.setFlagKey("  my-flag  ");

        when(repository.findByKey("my-flag")).thenReturn(Optional.empty());
        FeatureFlag saved = flagWithKey("my-flag");
        when(repository.save(any())).thenReturn(saved);

        service.upsert(request, actorId);

        verify(repository).findByKey("my-flag");
    }

    private FeatureFlag flagWithKey(String key) {
        FeatureFlag f = new FeatureFlag();
        f.setId(UUID.randomUUID());
        f.setFlagKey(key);
        f.setEnabled(false);
        return f;
    }
}
