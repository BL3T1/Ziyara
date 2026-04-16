package com.ziyara.backend.application.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Soft-deleted row surfaced for super-admin recycle search.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Deleted entity summary for admin restore")
public class DeletedItemResponse {

    @Schema(description = "USER or SERVICE", example = "USER")
    private String entityType;

    private UUID id;

    @Schema(description = "Primary display (email or service name)")
    private String label;

    @Schema(description = "Extra context (role, type, provider id, etc.)")
    private String detail;

    private LocalDateTime deletedAt;
}
