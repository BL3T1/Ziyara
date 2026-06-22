package com.ziyara.backend.application.service;

import com.ziyara.backend.application.dto.request.CreateIntegrationApiKeyRequest;
import com.ziyara.backend.application.dto.response.IntegrationApiKeyCreatedResponse;
import com.ziyara.backend.application.dto.response.IntegrationApiKeySummaryResponse;
import com.ziyara.backend.application.exception.ResourceNotFoundException;
import com.ziyara.backend.domain.entity.IntegrationApiKey;
import com.ziyara.backend.domain.repository.IntegrationApiKeyRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class IntegrationApiKeyServiceTest {

    @Mock IntegrationApiKeyRepository repository;
    @Mock PasswordEncoder passwordEncoder;
    @Mock AuditLogService auditLogService;

    IntegrationApiKeyService service;

    @BeforeEach
    void setUp() {
        service = new IntegrationApiKeyService(repository, passwordEncoder, auditLogService);
    }

    // ── listActive ────────────────────────────────────────────────────────────

    @Test
    void listActive_returnsMappedSummaries() {
        IntegrationApiKey key = apiKey("My Key");
        when(repository.findAllActive()).thenReturn(List.of(key));

        List<IntegrationApiKeySummaryResponse> result = service.listActive();

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getName()).isEqualTo("My Key");
    }

    @Test
    void listActive_emptyRepo_returnsEmpty() {
        when(repository.findAllActive()).thenReturn(List.of());

        assertThat(service.listActive()).isEmpty();
    }

    // ── create ────────────────────────────────────────────────────────────────

    @Test
    void create_generatesKeyWithZiyPrefix() {
        UUID actorId = UUID.randomUUID();
        CreateIntegrationApiKeyRequest request = new CreateIntegrationApiKeyRequest();
        request.setName("  Test Key  ");

        when(passwordEncoder.encode(any())).thenReturn("hashedSecret");
        IntegrationApiKey saved = apiKey("Test Key");
        when(repository.save(any())).thenReturn(saved);

        IntegrationApiKeyCreatedResponse result = service.create(request, actorId);

        assertThat(result.getPlainSecret()).startsWith("ziy_");
    }

    @Test
    void create_trimsName_andAuditsAction() {
        UUID actorId = UUID.randomUUID();
        CreateIntegrationApiKeyRequest request = new CreateIntegrationApiKeyRequest();
        request.setName("  Trimmed Name  ");

        when(passwordEncoder.encode(any())).thenReturn("hash");
        IntegrationApiKey saved = apiKey("Trimmed Name");
        when(repository.save(any())).thenReturn(saved);

        service.create(request, actorId);

        ArgumentCaptor<IntegrationApiKey> captor = ArgumentCaptor.forClass(IntegrationApiKey.class);
        verify(repository).save(captor.capture());
        assertThat(captor.getValue().getName()).isEqualTo("Trimmed Name");
        verify(auditLogService).logAction(any(), any(), any(), eq(actorId), any(), any(), any(), any());
    }

    // ── revoke ────────────────────────────────────────────────────────────────

    @Test
    void revoke_keyNotFound_throwsResourceNotFound() {
        UUID id = UUID.randomUUID();
        when(repository.findById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.revoke(id, UUID.randomUUID()))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("API key not found");
    }

    @Test
    void revoke_alreadyRevoked_idempotent() {
        UUID id = UUID.randomUUID();
        IntegrationApiKey key = apiKey("Key");
        key.setRevokedAt(Instant.now());
        when(repository.findById(id)).thenReturn(Optional.of(key));

        service.revoke(id, UUID.randomUUID());

        verify(repository, never()).save(any());
    }

    @Test
    void revoke_activeKey_setsRevokedAt() {
        UUID id = UUID.randomUUID();
        IntegrationApiKey key = apiKey("Key");
        key.setRevokedAt(null);
        when(repository.findById(id)).thenReturn(Optional.of(key));
        when(repository.save(any())).thenReturn(key);

        service.revoke(id, UUID.randomUUID());

        assertThat(key.getRevokedAt()).isNotNull();
        verify(repository).save(key);
    }

    private IntegrationApiKey apiKey(String name) {
        IntegrationApiKey k = new IntegrationApiKey();
        k.setId(UUID.randomUUID());
        k.setName(name);
        k.setKeyPrefix("ziy_abc1234567890123");
        k.setSecretHash("hash");
        k.setCreatedAt(Instant.now());
        return k;
    }
}
