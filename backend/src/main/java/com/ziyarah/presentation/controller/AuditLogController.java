package com.ziyarah.presentation.controller;

import com.ziyarah.application.dto.ApiResponse;
import com.ziyarah.application.dto.response.AuditLogResponse;
import com.ziyarah.application.service.AuditLogService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
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
@Tag(name = "Audit Logs", description = "System activity and audit trail APIs")
@SecurityRequirement(name = "bearerAuth")
public class AuditLogController {
    
    private final AuditLogService auditLogService;
    
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
}
