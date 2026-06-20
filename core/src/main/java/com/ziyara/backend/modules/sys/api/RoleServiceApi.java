package com.ziyara.backend.modules.sys.api;

import com.ziyara.backend.application.dto.request.CreateGroupRequest;
import com.ziyara.backend.application.dto.request.UpdateGroupRequest;
import com.ziyara.backend.application.dto.request.CreateRoleRequest;
import com.ziyara.backend.application.dto.request.DeleteRoleRequest;
import com.ziyara.backend.application.dto.request.UpdateRoleNavigationRequest;
import com.ziyara.backend.application.dto.request.UpdateRoleRequest;
import com.ziyara.backend.application.dto.request.UpdateRolePermissionsRequest;
import com.ziyara.backend.application.dto.response.GroupResponse;
import com.ziyara.backend.application.dto.response.GroupSummaryResponse;
import com.ziyara.backend.application.dto.response.PermissionSummaryResponse;
import com.ziyara.backend.application.dto.UserResponse;
import com.ziyara.backend.application.dto.response.RoleResponse;

import org.springframework.data.domain.Page;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Sys module API: role and permission management (Phase 3).
 */
public interface RoleServiceApi {

    List<RoleResponse> listRoles();

    Optional<RoleResponse> getRole(UUID id);

    List<PermissionSummaryResponse> getPermissionCatalogue();

    List<PermissionSummaryResponse> getUnlockedPermissions();

    List<GroupResponse> getGroups();

    GroupResponse createGroup(CreateGroupRequest request, UUID currentUserId);

    GroupResponse updateGroup(UUID groupId, UpdateGroupRequest request, UUID currentUserId);

    void deleteGroup(UUID groupId, UUID currentUserId);

    List<GroupSummaryResponse> listGroupSummaries();

    /** Users whose primary role code or RBAC assignment matches a role in the group (or ungrouped). */
    Page<UserResponse> listGroupMembers(UUID groupId, int page, int size);

    /** Users assigned to a specific role (by RBAC assignment). */
    Page<UserResponse> listRoleMembers(UUID roleId, int page, int size);

    RoleResponse createCustomRole(CreateRoleRequest request, UUID currentUserId);

    RoleResponse updateRolePermissions(UUID roleId, UpdateRolePermissionsRequest request, UUID currentUserId);

    RoleResponse updateRole(UUID roleId, UpdateRoleRequest request, UUID currentUserId);

    void deleteRole(UUID roleId, DeleteRoleRequest request, UUID currentUserId);

    RoleResponse updateRoleNavigation(UUID roleId, UpdateRoleNavigationRequest request, UUID currentUserId);
}
