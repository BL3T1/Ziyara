package com.ziyara.backend.application.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Integration API key metadata (no secret)")
public class IntegrationApiKeySummaryResponse {

    private UUID id;
    private String name;
    private String keyPrefix;
    private Instant createdAt;
    private Instant revokedAt;
    private Instant lastUsedAt;
}
