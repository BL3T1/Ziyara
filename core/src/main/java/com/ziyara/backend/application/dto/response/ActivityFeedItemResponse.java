package com.ziyara.backend.application.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Single activity feed item for dashboard live feed (DASHBOARD_DESIGN_REPORT).
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Activity feed item")
public class ActivityFeedItemResponse {

    @Schema(description = "Audit log ID")
    private String id;

    @Schema(description = "Action performed")
    private String action;

    @Schema(description = "Entity type")
    private String entityType;

    @Schema(description = "Entity ID")
    private String entityId;

    @Schema(description = "User display (e.g. email)")
    private String userDisplay;

    @Schema(description = "Summary of change (old â†’ new)")
    private String changeSummary;

    @Schema(description = "Timestamp")
    private LocalDateTime timestamp;
}
