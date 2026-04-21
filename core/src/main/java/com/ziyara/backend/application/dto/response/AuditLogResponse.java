package com.ziyara.backend.application.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Audit log entry details")
public class AuditLogResponse {
    
    @Schema(description = "Log ID")
    private UUID id;
    
    @Schema(description = "Performed action")
    private String action;
    
    @Schema(description = "Affected entity name")
    private String entityName;
    
    @Schema(description = "Affected entity ID")
    private String entityId;
    
    @Schema(description = "User who performed action")
    private UUID userId;

    @Schema(description = "Request correlation id when present")
    private String correlationId;
    
    @Schema(description = "Creation timestamp")
    private LocalDateTime createdAt;

    @Schema(description = "User display (e.g. email) for dashboard")
    private String userDisplay;

    @Schema(description = "Resource path for display (entity/id)")
    private String resource;

    @Schema(description = "Status for display (e.g. Success, Failed)")
    private String status;
}
