package com.ziyara.backend.presentation.controller;

import com.ziyara.backend.application.dto.ApiResponse;
import com.ziyara.backend.application.service.AdminActivityLogService;
import com.ziyara.backend.infrastructure.security.ApiAuthorizationExpressions;
import com.ziyara.backend.infrastructure.security.JwtService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.jooq.DSLContext;
import org.jooq.impl.DSL;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/admin/permissions")
@RequiredArgsConstructor
@Tag(name = "Admin Permissions", description = "Permission matrix management")
@SecurityRequirement(name = "bearerAuth")
public class AdminPermissionsController {

    private static final String TABLE = "sys_permission_matrix";

    private final DSLContext dsl;
    private final JwtService jwtService;
    private final AdminActivityLogService activityLogService;

    @GetMapping("/matrix")
    @PreAuthorize(ApiAuthorizationExpressions.ROLES_READ)
    @Operation(summary = "Get full permission matrix", description = "Returns all role × module × action entries")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> getMatrix() {
        var rows = dsl.select()
                .from(DSL.table(DSL.name(TABLE)))
                .orderBy(
                        DSL.field(DSL.name("role_id")),
                        DSL.field(DSL.name("module")),
                        DSL.field(DSL.name("action"))
                )
                .fetchMaps();
        return ResponseEntity.ok(ApiResponse.success(rows));
    }

    @PostMapping("/matrix")
    @PreAuthorize(ApiAuthorizationExpressions.ROLES_WRITE)
    @Operation(summary = "Upsert permission entry", description = "Insert or update a single role × module × action permission")
    public ResponseEntity<ApiResponse<String>> upsertPermission(
            @RequestBody Map<String, Object> body,
            @RequestHeader("Authorization") String authHeader
    ) {
        UUID roleId = UUID.fromString((String) body.get("roleId"));
        String module = (String) body.get("module");
        String action = (String) body.get("action");
        boolean granted = Boolean.TRUE.equals(body.get("granted"));
        UUID adminId = extractUserId(authHeader);

        dsl.insertInto(DSL.table(DSL.name(TABLE)))
                .columns(
                        DSL.field(DSL.name("id")),
                        DSL.field(DSL.name("role_id")),
                        DSL.field(DSL.name("module")),
                        DSL.field(DSL.name("action")),
                        DSL.field(DSL.name("granted")),
                        DSL.field(DSL.name("updated_by")),
                        DSL.field(DSL.name("updated_at"))
                )
                .values(UUID.randomUUID(), roleId, module, action, granted, adminId, OffsetDateTime.now())
                .onConflict(
                        DSL.field(DSL.name("role_id")),
                        DSL.field(DSL.name("module")),
                        DSL.field(DSL.name("action"))
                )
                .doUpdate()
                .set(DSL.field(DSL.name("granted")), granted)
                .set(DSL.field(DSL.name("updated_by")), adminId)
                .set(DSL.field(DSL.name("updated_at")), OffsetDateTime.now())
                .execute();

        activityLogService.log(adminId, "PERMISSION_UPDATED",
                "PERMISSION_MATRIX", roleId + "/" + module + "/" + action,
                "Set " + module + ":" + action + " = " + granted + " for role " + roleId);

        return ResponseEntity.ok(ApiResponse.success("Permission updated"));
    }

    @PutMapping("/roles/{roleId}")
    @PreAuthorize(ApiAuthorizationExpressions.ROLES_WRITE)
    @Operation(summary = "Bulk update role permissions", description = "Replace all permissions for a role with the provided list")
    public ResponseEntity<ApiResponse<String>> updateRolePermissions(
            @PathVariable UUID roleId,
            @RequestBody List<Map<String, Object>> permissions,
            @RequestHeader("Authorization") String authHeader
    ) {
        UUID adminId = extractUserId(authHeader);

        dsl.transaction(ctx -> {
            DSLContext tx = DSL.using(ctx);
            tx.deleteFrom(DSL.table(DSL.name(TABLE)))
                    .where(DSL.field(DSL.name("role_id")).eq(roleId))
                    .execute();
            for (Map<String, Object> perm : permissions) {
                String module = (String) perm.get("module");
                String action = (String) perm.get("action");
                boolean granted = Boolean.TRUE.equals(perm.get("granted"));
                tx.insertInto(DSL.table(DSL.name(TABLE)))
                        .columns(
                                DSL.field(DSL.name("id")),
                                DSL.field(DSL.name("role_id")),
                                DSL.field(DSL.name("module")),
                                DSL.field(DSL.name("action")),
                                DSL.field(DSL.name("granted")),
                                DSL.field(DSL.name("updated_by")),
                                DSL.field(DSL.name("updated_at"))
                        )
                        .values(UUID.randomUUID(), roleId, module, action, granted, adminId, OffsetDateTime.now())
                        .execute();
            }
        });

        activityLogService.log(adminId, "ROLE_PERMISSIONS_REPLACED", "ROLE", roleId.toString(),
                "Replaced all permissions for role " + roleId + " (" + permissions.size() + " entries)");

        return ResponseEntity.ok(ApiResponse.success("Role permissions updated"));
    }

    private UUID extractUserId(String authHeader) {
        return UUID.fromString(jwtService.extractUserId(authHeader.substring(7)));
    }
}
