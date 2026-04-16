package com.ziyara.backend.presentation.controller;

import com.ziyara.backend.application.dto.ApiResponse;
import com.ziyara.backend.application.dto.response.AuditLogResponse;
import com.ziyara.backend.infrastructure.security.ApiAuthorizationExpressions;
import com.ziyara.backend.modules.sys.api.AuditServiceApi;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/**
 * Controller: AuditLogController
 * Handles system activity audit queries
 */
@RestController
@RequestMapping("/audit-logs")
@RequiredArgsConstructor
@PreAuthorize(ApiAuthorizationExpressions.COMPANY_STAFF)
@Tag(name = "Audit Logs", description = "System activity and audit trail APIs")
@SecurityRequirement(name = "bearerAuth")
public class AuditLogController {
    
    private final AuditServiceApi auditLogService;
    
    @GetMapping("/entity/{name}/{id}")
    @Operation(summary = "Get entity history", description = "Retrieve all audit logs for a specific entity")
    public ResponseEntity<ApiResponse<List<AuditLogResponse>>> getEntityLogs(
            @PathVariable String name,
            @PathVariable String id
    ) {
        return ResponseEntity.ok(ApiResponse.success(auditLogService.getEntityLogs(name, id)));
    }
    
    @GetMapping("/user/{userId}")
    @Operation(summary = "Get user activity", description = "Retrieve all actions performed by a specific user")
    public ResponseEntity<ApiResponse<List<AuditLogResponse>>> getUserLogs(@PathVariable UUID userId) {
        return ResponseEntity.ok(ApiResponse.success(auditLogService.getUserLogs(userId)));
    }

    @GetMapping
    @Operation(summary = "Get recent audit logs", description = "List recent audit logs for dashboard (searchable)")
    public ResponseEntity<ApiResponse<List<AuditLogResponse>>> getRecentLogs(
            @RequestParam(defaultValue = "20") int limit,
            @RequestParam(required = false) String search) {
        return ResponseEntity.ok(ApiResponse.success(auditLogService.getRecentLogs(limit, search)));
    }
}
