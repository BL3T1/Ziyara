package com.ziyara.backend.application.service;

import com.ziyara.backend.application.dto.request.CreateIntegrationApiKeyRequest;
import com.ziyara.backend.application.dto.response.IntegrationApiKeyCreatedResponse;
import com.ziyara.backend.application.dto.response.IntegrationApiKeySummaryResponse;
import com.ziyara.backend.domain.entity.IntegrationApiKey;
import com.ziyara.backend.domain.repository.IntegrationApiKeyRepository;
import com.ziyara.backend.application.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.Instant;
import java.util.HexFormat;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class IntegrationApiKeyService {

    private static final String KEY_PREFIX = "ziy_";
    private static final int RANDOM_BYTES = 24;

    private final IntegrationApiKeyRepository repository;
    private final PasswordEncoder passwordEncoder;
    private final AuditLogService auditLogService;
    private final SecureRandom secureRandom = new SecureRandom();

    @Transactional(readOnly = true)
    public List<IntegrationApiKeySummaryResponse> listActive() {
        return repository.findAllActive().stream()
                .map(this::toSummary)
                .collect(Collectors.toList());
    }

    @Transactional
    public IntegrationApiKeyCreatedResponse create(CreateIntegrationApiKeyRequest request, UUID actorId) {
        String name = request.getName().trim();
        byte[] raw = new byte[RANDOM_BYTES];
        secureRandom.nextBytes(raw);
        String suffix = HexFormat.of().formatHex(raw);
        String plainSecret = KEY_PREFIX + suffix;
        String keyPrefix = plainSecret.substring(0, Math.min(16, plainSecret.length()));
        IntegrationApiKey key = new IntegrationApiKey();
        key.setName(name);
        key.setKeyPrefix(keyPrefix);
        key.setSecretHash(passwordEncoder.encode(plainSecret));
        IntegrationApiKey saved = repository.save(key);
        auditLogService.logAction(
                "INTEGRATION_API_KEY_CREATE",
                "IntegrationApiKey",
                saved.getId().toString(),
                actorId,
                null,
                "name=" + name,
                null,
                null
        );
        return IntegrationApiKeyCreatedResponse.builder()
                .id(saved.getId())
                .name(saved.getName())
                .keyPrefix(saved.getKeyPrefix())
                .createdAt(saved.getCreatedAt())
                .plainSecret(plainSecret)
                .build();
    }

    @Transactional
    public void revoke(UUID id, UUID actorId) {
        IntegrationApiKey key = repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("API key not found"));
        if (key.getRevokedAt() != null) {
            return;
        }
        key.setRevokedAt(Instant.now());
        repository.save(key);
        auditLogService.logAction(
                "INTEGRATION_API_KEY_REVOKE",
                "IntegrationApiKey",
                id.toString(),
                actorId,
                null,
                "revoked",
                null,
                null
        );
    }

    private IntegrationApiKeySummaryResponse toSummary(IntegrationApiKey k) {
        return IntegrationApiKeySummaryResponse.builder()
                .id(k.getId())
                .name(k.getName())
                .keyPrefix(k.getKeyPrefix())
                .createdAt(k.getCreatedAt())
                .revokedAt(k.getRevokedAt())
                .lastUsedAt(k.getLastUsedAt())
                .build();
    }
}
