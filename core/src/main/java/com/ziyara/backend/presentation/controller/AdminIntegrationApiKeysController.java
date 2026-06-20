package com.ziyara.backend.presentation.controller;

import com.ziyara.backend.application.dto.ApiResponse;
import com.ziyara.backend.application.dto.request.CreateIntegrationApiKeyRequest;
import com.ziyara.backend.application.dto.response.IntegrationApiKeyCreatedResponse;
import com.ziyara.backend.application.dto.response.IntegrationApiKeySummaryResponse;
import com.ziyara.backend.application.service.IntegrationApiKeyService;
import com.ziyara.backend.infrastructure.security.ApiAuthorizationExpressions;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/admin/integration-api-keys")
@RequiredArgsConstructor
@Tag(name = "Admin Integration API Keys", description = "Hashed secrets for outbound integrations (Super Admin only)")
@SecurityRequirement(name = "bearerAuth")
public class AdminIntegrationApiKeysController {

    private final IntegrationApiKeyService integrationApiKeyService;

    @GetMapping
    @PreAuthorize(ApiAuthorizationExpressions.SETTINGS_READ)
    @Operation(summary = "List active integration API keys")
    public ResponseEntity<ApiResponse<List<IntegrationApiKeySummaryResponse>>> list() {
        return ResponseEntity.ok(ApiResponse.success(integrationApiKeyService.listActive()));
    }

    @PostMapping
    @PreAuthorize(ApiAuthorizationExpressions.SETTINGS_WRITE)
    @Operation(summary = "Create API key", description = "Returns plain secret once")
    public ResponseEntity<ApiResponse<IntegrationApiKeyCreatedResponse>> create(
            @Valid @RequestBody CreateIntegrationApiKeyRequest request) {
        UUID userId = getCurrentUserId();
        IntegrationApiKeyCreatedResponse created = integrationApiKeyService.create(request, userId);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("API key created — copy the secret now; it will not be shown again", created));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize(ApiAuthorizationExpressions.SETTINGS_WRITE)
    @Operation(summary = "Revoke API key")
    public ResponseEntity<ApiResponse<Void>> revoke(@PathVariable UUID id) {
        UUID userId = getCurrentUserId();
        integrationApiKeyService.revoke(id, userId);
        return ResponseEntity.ok(ApiResponse.success("API key revoked", null));
    }

    private static UUID getCurrentUserId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getName() != null) {
            try {
                return UUID.fromString(auth.getName());
            } catch (IllegalArgumentException ignored) {
            }
        }
        return null;
    }
}
