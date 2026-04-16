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
@Schema(description = "Named feature toggle")
public class FeatureFlagResponse {

    private UUID id;
    private String flagKey;
    private boolean enabled;
    private String description;
    private Instant updatedAt;
    private UUID updatedBy;
}
