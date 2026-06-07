package com.ziyara.backend.presentation.controller;

import com.ziyara.backend.application.dto.ApiResponse;
import com.ziyara.backend.application.dto.response.AuditLogResponse;
import static com.ziyara.backend.infrastructure.security.ApiAuthorizationExpressions.*;
import com.ziyara.backend.modules.sys.api.AuditServiceApi;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Controller: AuditLogController
 * Handles system activity audit queries.
 * All endpoints are gated to COMPANY_STAFF roles.
 */
@RestController
@RequestMapping("/audit-logs")
@RequiredArgsConstructor
@PreAuthorize(COMPANY_STAFF)
@Tag(name = "Audit Logs", description = "System activity and audit trail APIs")
@SecurityRequirement(name = "bearerAuth")
public class AuditLogController {

    private final AuditServiceApi auditLogService;

    // -------------------------------------------------------------------------
    // Existing endpoints (unchanged behaviour)
    // -------------------------------------------------------------------------

    @GetMapping("/entity/{name}/{id}")
    @Operation(summary = "Get entity history",
               description = "Retrieve all audit logs for a specific entity")
    public ResponseEntity<ApiResponse<List<AuditLogResponse>>> getEntityLogs(
            @PathVariable String name,
            @PathVariable String id) {
        return ResponseEntity.ok(ApiResponse.success(auditLogService.getEntityLogs(name, id)));
    }

    @GetMapping("/user/{userId}")
    @Operation(summary = "Get user activity",
               description = "Retrieve all actions performed by a specific user")
    public ResponseEntity<ApiResponse<List<AuditLogResponse>>> getUserLogs(
            @PathVariable UUID userId) {
        return ResponseEntity.ok(ApiResponse.success(auditLogService.getUserLogs(userId)));
    }

    @GetMapping
    @Operation(summary = "Get recent audit logs",
               description = "Recent audit logs for the dashboard. Supports keyword search.")
    public ResponseEntity<ApiResponse<List<AuditLogResponse>>> getRecentLogs(
            @RequestParam(defaultValue = "20") int limit,
            @RequestParam(required = false) String search) {
        return ResponseEntity.ok(ApiResponse.success(auditLogService.getRecentLogs(limit, search)));
    }

    // -------------------------------------------------------------------------
    // New: filtered / paginated endpoint (powers the deletion-log component)
    // -------------------------------------------------------------------------

    @GetMapping("/filter")
    @Operation(
        summary = "Filtered audit log search",
        description = "Server-side filtered and paginated query. All filter parameters are optional. "
                    + "To view deletion logs, pass action=EMPLOYEE_OFFBOARDED or action=DELETE. "
                    + "Dates are ISO-8601 local date-time, e.g. 2026-01-15T00:00:00.")
    public ResponseEntity<ApiResponse<Page<AuditLogResponse>>> getFilteredLogs(
            @Parameter(description = "Filter by entity type, e.g. Employee, User, ServiceProvider")
            @RequestParam(required = false) String entityType,

            @Parameter(description = "Filter by action name, e.g. DELETE, EMPLOYEE_OFFBOARDED")
            @RequestParam(required = false) String action,

            @Parameter(description = "Filter by the user who performed the action (UUID)")
            @RequestParam(required = false) UUID userId,

            @Parameter(description = "Earliest log timestamp (inclusive), ISO-8601")
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime dateFrom,

            @Parameter(description = "Latest log timestamp (inclusive), ISO-8601")
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime dateTo,

            @RequestParam(defaultValue = "0")  int page,
            @RequestParam(defaultValue = "20") int size) {

        Page<AuditLogResponse> result = auditLogService.getFilteredLogs(
                entityType, action, userId, dateFrom, dateTo, page, size);
        return ResponseEntity.ok(ApiResponse.success(result));
    }

    /**
     * Convenience endpoint for the deletion-log UI component.
     * Pre-applies action=EMPLOYEE_OFFBOARDED but still accepts all other filters.
     */
    @GetMapping("/deletions")
    @Operation(
        summary = "Deletion / offboarding audit log",
        description = "Pre-filtered view of deletion and offboarding events. "
                    + "Additional filters (entityType, userId, date range) can narrow results further.")
    public ResponseEntity<ApiResponse<Page<AuditLogResponse>>> getDeletionLogs(
            @Parameter(description = "Further filter by entity type")
            @RequestParam(required = false) String entityType,

            @Parameter(description = "Override the default action filter (default: EMPLOYEE_OFFBOARDED)")
            @RequestParam(required = false) String action,

            @Parameter(description = "Filter by actor user ID")
            @RequestParam(required = false) UUID userId,

            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime dateFrom,

            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime dateTo,

            @RequestParam(defaultValue = "0")  int page,
            @RequestParam(defaultValue = "20") int size) {

        // Default to deletion/offboarding actions when no override is supplied
        String effectiveAction = (action != null && !action.isBlank())
                ? action
                : "EMPLOYEE_OFFBOARDED";

        Page<AuditLogResponse> result = auditLogService.getFilteredLogs(
                entityType, effectiveAction, userId, dateFrom, dateTo, page, size);
        return ResponseEntity.ok(ApiResponse.success(result));
    }
}
