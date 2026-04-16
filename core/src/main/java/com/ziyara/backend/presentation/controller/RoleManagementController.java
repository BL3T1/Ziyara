package com.ziyara.backend.presentation.controller;

import com.ziyara.backend.application.dto.ApiResponse;
import com.ziyara.backend.application.dto.request.CreateGroupRequest;
import com.ziyara.backend.application.dto.request.CreateRoleRequest;
import com.ziyara.backend.application.dto.request.DeleteRoleRequest;
import com.ziyara.backend.application.dto.request.UpdateRoleNavigationRequest;
import com.ziyara.backend.application.dto.request.UpdateRoleRequest;
import com.ziyara.backend.application.dto.request.UpdateRolePermissionsRequest;
import com.ziyara.backend.application.dto.UserResponse;
import com.ziyara.backend.application.dto.response.GroupResponse;
import com.ziyara.backend.application.dto.response.GroupSummaryResponse;
import com.ziyara.backend.application.dto.response.PermissionSummaryResponse;
import com.ziyara.backend.application.dto.response.RoleResponse;
import com.ziyara.backend.modules.sys.api.RoleServiceApi;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/**
 * Role Management (ROLE_MANAGEMENT_REPORT). Super Admin only.
 */
@RestController
@RequestMapping("/roles")
@RequiredArgsConstructor
@Tag(name = "Role Management", description = "RBAC role and permission management (Super Admin only)")
@SecurityRequirement(name = "bearerAuth")
@PreAuthorize("hasRole('SUPER_ADMIN')")
public class RoleManagementController {

    private final RoleServiceApi roleManagementService;

    @GetMapping
    @Operation(summary = "List all roles", description = "Returns roles with group, permissions, and user count")
    public ResponseEntity<ApiResponse<List<RoleResponse>>> listRoles() {
        return ResponseEntity.ok(ApiResponse.success(roleManagementService.listRoles()));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get role by ID")
    public ResponseEntity<ApiResponse<RoleResponse>> getRole(@PathVariable UUID id) {
        return roleManagementService.getRole(id)
                .map(r -> ResponseEntity.ok(ApiResponse.success(r)))
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/permissions/catalogue")
    @Operation(summary = "Permission catalogue", description = "All permissions. Locked permissions may be assigned only to system roles (PUT /{id}/permissions); custom roles reject locked ids.")
    public ResponseEntity<ApiResponse<List<PermissionSummaryResponse>>> getPermissionCatalogue() {
        return ResponseEntity.ok(ApiResponse.success(roleManagementService.getPermissionCatalogue()));
    }

    @GetMapping("/permissions/unlocked")
    @Operation(summary = "Unlocked permissions", description = "Permissions that can be assigned to custom roles")
    public ResponseEntity<ApiResponse<List<PermissionSummaryResponse>>> getUnlockedPermissions() {
        return ResponseEntity.ok(ApiResponse.success(roleManagementService.getUnlockedPermissions()));
    }

    @GetMapping("/groups")
    @Operation(summary = "List groups", description = "Organizational groups (G1-G7)")
    public ResponseEntity<ApiResponse<List<GroupResponse>>> getGroups() {
        return ResponseEntity.ok(ApiResponse.success(roleManagementService.getGroups()));
    }

    @PostMapping("/groups")
    @Operation(summary = "Create organizational group", description = "Adds sys_groups row; code optional (auto next G{n})")
    public ResponseEntity<ApiResponse<GroupResponse>> createGroup(
            @Valid @RequestBody CreateGroupRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {
        UUID currentUserId = userDetails != null ? UUID.fromString(userDetails.getUsername()) : null;
        GroupResponse created = roleManagementService.createGroup(request, currentUserId);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success("Group created", created));
    }

    @GetMapping("/groups/summary")
    @Operation(summary = "List group summaries", description = "Groups with role count and total staff per group (for directory overview)")
    public ResponseEntity<ApiResponse<List<GroupSummaryResponse>>> listGroupSummaries() {
        return ResponseEntity.ok(ApiResponse.success(roleManagementService.listGroupSummaries()));
    }

    @GetMapping("/groups/{groupId}/users")
    @Operation(summary = "List users in group", description = "Paginated users whose primary role or RBAC assignment matches a role in the group")
    public ResponseEntity<ApiResponse<Page<UserResponse>>> listGroupMembers(
            @PathVariable UUID groupId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(ApiResponse.success(roleManagementService.listGroupMembers(groupId, page, size)));
    }

    @PostMapping
    @Operation(summary = "Create custom role", description = "Creates a role with is_system_role=false")
    public ResponseEntity<ApiResponse<RoleResponse>> createRole(
            @Valid @RequestBody CreateRoleRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {
        UUID currentUserId = userDetails != null ? UUID.fromString(userDetails.getUsername()) : null;
        RoleResponse created = roleManagementService.createCustomRole(request, currentUserId);
        return ResponseEntity.ok(ApiResponse.success(created));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update role name/description", description = "Display fields only; use PUT /{id}/permissions for permission sets. Applies to system and custom roles.")
    public ResponseEntity<ApiResponse<RoleResponse>> updateRole(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateRoleRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {
        UUID currentUserId = userDetails != null ? UUID.fromString(userDetails.getUsername()) : null;
        RoleResponse updated = roleManagementService.updateRole(id, request, currentUserId);
        return ResponseEntity.ok(ApiResponse.success("Role updated", updated));
    }

    @PutMapping("/{id}/navigation")
    @Operation(summary = "Update role sidebar navigation", description = "Super Admin: ordered dashboard nav item IDs (system and custom roles)")
    public ResponseEntity<ApiResponse<RoleResponse>> updateRoleNavigation(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateRoleNavigationRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {
        UUID currentUserId = userDetails != null ? UUID.fromString(userDetails.getUsername()) : null;
        RoleResponse updated = roleManagementService.updateRoleNavigation(id, request, currentUserId);
        return ResponseEntity.ok(ApiResponse.success(updated));
    }

    @PutMapping("/{id}/permissions")
    @Operation(summary = "Update role permissions", description = "Replace all permissions for the role. System roles: any permission including locked. Custom roles: unlocked only; locked ids return 400. Unknown permission ids return 400.")
    public ResponseEntity<ApiResponse<RoleResponse>> updateRolePermissions(
            @PathVariable UUID id,
            @RequestBody UpdateRolePermissionsRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {
        UUID currentUserId = userDetails != null ? UUID.fromString(userDetails.getUsername()) : null;
        RoleResponse updated = roleManagementService.updateRolePermissions(id, request, currentUserId);
        return ResponseEntity.ok(ApiResponse.success(updated));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete custom role", description = "If users are assigned, provide targetRoleId in body for reassignment")
    public ResponseEntity<ApiResponse<Void>> deleteRole(
            @PathVariable UUID id,
            @RequestBody(required = false) DeleteRoleRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {
        UUID currentUserId = userDetails != null ? UUID.fromString(userDetails.getUsername()) : null;
        roleManagementService.deleteRole(id, request != null ? request : new DeleteRoleRequest(), currentUserId);
        return ResponseEntity.ok(ApiResponse.success("Role deleted", null));
    }
}
