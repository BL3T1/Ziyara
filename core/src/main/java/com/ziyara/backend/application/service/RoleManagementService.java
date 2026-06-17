package com.ziyara.backend.application.service;

import com.ziyara.backend.application.dto.request.CreateGroupRequest;
import com.ziyara.backend.application.dto.request.UpdateGroupRequest;
import com.ziyara.backend.application.dto.request.CreateRoleRequest;
import com.ziyara.backend.application.dto.request.DeleteRoleRequest;
import com.ziyara.backend.application.dto.request.UpdateRoleNavigationRequest;
import com.ziyara.backend.application.dto.request.UpdateRoleRequest;
import com.ziyara.backend.application.dto.request.UpdateRolePermissionsRequest;
import com.ziyara.backend.application.navigation.CompanySidebarCatalog;
import com.ziyara.backend.application.dto.response.GroupResponse;
import com.ziyara.backend.application.dto.response.GroupSummaryResponse;
import com.ziyara.backend.application.dto.response.PermissionSummaryResponse;
import com.ziyara.backend.application.dto.UserResponse;
import com.ziyara.backend.application.dto.response.RoleResponse;
import com.ziyara.backend.application.locale.RequestLocaleHolder;
import com.ziyara.backend.domain.entity.Group;
import com.ziyara.backend.domain.entity.Permission;
import com.ziyara.backend.domain.entity.Role;
import com.ziyara.backend.domain.enums.RoleLevel;
import com.ziyara.backend.domain.enums.RoleStatus;
import com.ziyara.backend.domain.repository.*;
import com.ziyara.backend.modules.sys.api.AuditServiceApi;
import com.ziyara.backend.modules.sys.api.RoleServiceApi;
import com.ziyara.backend.application.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Role management (ROLE_MANAGEMENT_REPORT). Super Admin only. Implements RoleServiceApi.
 * Group operations are delegated to {@link GroupManagementService}.
 * Permission catalogue operations are delegated to {@link PermissionQueryService}.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class RoleManagementService implements RoleServiceApi {

    private final RoleRepository roleRepository;
    private final PermissionRepository permissionRepository;
    private final GroupRepository groupRepository;
    private final RolePermissionRepository rolePermissionRepository;
    private final UserRoleAssignmentRepository userRoleAssignmentRepository;
    private final UserRepository userRepository;
    private final AuditServiceApi auditLogService;
    private final GroupManagementService groupManagementService;
    private final PermissionQueryService permissionQueryService;

    private static final String ROLE_ENTITY = "roles";

    // ── Role queries ─────────────────────────────────────────────────────────────

    @Cacheable(value = "staffRoleCatalog",
               key = "T(com.ziyara.backend.application.locale.RequestLocaleHolder).getLocale().getLanguage()")
    @Transactional(readOnly = true)
    public List<RoleResponse> listRoles() {
        List<Role> roles = roleRepository.findAllOrderByName();
        Map<UUID, String> groupNames = groupRepository.findAll().stream()
                .collect(Collectors.toMap(Group::getId, g -> RequestLocaleHolder.localized(g.getName(), g.getNameAr()), (a, b) -> a));
        Map<UUID, Permission> permById = permissionMap();
        Set<UUID> roleIds = roles.stream().map(Role::getId).collect(Collectors.toSet());
        Map<UUID, Set<UUID>> permIdsByRole = rolePermissionRepository.findPermissionIdsByRoleIds(roleIds);
        return roles.stream()
                .map(r -> toRoleResponse(r, groupNames, permById, permIdsByRole))
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public Optional<RoleResponse> getRole(UUID id) {
        Map<UUID, Permission> permById = permissionMap();
        return roleRepository.findById(id)
                .map(r -> toRoleResponse(r, groupNamesMap(), permById));
    }

    // ── Permission catalogue — delegated ─────────────────────────────────────────

    @Override
    public List<PermissionSummaryResponse> getPermissionCatalogue() {
        return permissionQueryService.getPermissionCatalogue();
    }

    @Override
    public List<PermissionSummaryResponse> getUnlockedPermissions() {
        return permissionQueryService.getUnlockedPermissions();
    }

    // ── Group operations — delegated ─────────────────────────────────────────────

    @Override
    public List<GroupResponse> getGroups() {
        return groupManagementService.getGroups();
    }

    @Override
    public GroupResponse createGroup(CreateGroupRequest request, UUID currentUserId) {
        return groupManagementService.createGroup(request, currentUserId);
    }

    @Override
    public GroupResponse updateGroup(UUID groupId, UpdateGroupRequest request, UUID currentUserId) {
        return groupManagementService.updateGroup(groupId, request, currentUserId);
    }

    @Override
    public void deleteGroup(UUID groupId, UUID currentUserId) {
        groupManagementService.deleteGroup(groupId, currentUserId);
    }

    @Override
    public List<GroupSummaryResponse> listGroupSummaries() {
        return groupManagementService.listGroupSummaries();
    }

    @Override
    public Page<UserResponse> listGroupMembers(UUID groupId, int page, int size) {
        return groupManagementService.listGroupMembers(groupId, page, size);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<UserResponse> listRoleMembers(UUID roleId, int page, int size) {
        List<UUID> userIds = userRoleAssignmentRepository.findUserIdsByRoleId(roleId);
        int total = userIds.size();
        int from = Math.min(page * size, total);
        int to = Math.min(from + size, total);
        List<UUID> pageIds = userIds.subList(from, to);
        List<UserResponse> users = userRepository.findAllById(pageIds).stream()
                .map(u -> {
                    UserResponse r = new UserResponse();
                    r.setId(u.getId());
                    r.setEmail(u.getEmail());
                    r.setRole(u.getRole());
                    return r;
                })
                .collect(Collectors.toList());
        return new org.springframework.data.domain.PageImpl<>(users,
                org.springframework.data.domain.PageRequest.of(page, size),
                total);
    }

    // ── Custom role CRUD ─────────────────────────────────────────────────────────

    @Transactional
    @CacheEvict(cacheNames = "staffRoleCatalog", allEntries = true)
    public RoleResponse createCustomRole(CreateRoleRequest request, UUID currentUserId) {
        Map<UUID, Permission> permById = permissionMap();
        if (request.getPermissionIds() != null && !request.getPermissionIds().isEmpty()) {
            validateAllPermissionsExist(request.getPermissionIds(), permById.keySet());
        }
        String code = generateCustomRoleCode(request.getName());
        if (roleRepository.existsByCode(code)) {
            throw new IllegalArgumentException("A role with generated code already exists; try a different name.");
        }
        Role role = new Role();
        UUID resolvedGroupId = resolveGroupForNewRole(request, currentUserId);
        role.setName(request.getName().trim());
        role.setCode(code);
        role.setDescription(request.getDescription() != null ? request.getDescription().trim() : null);
        role.setLevel(RoleLevel.EMPLOYEE);
        role.setGroupId(resolvedGroupId);
        role.setSystemRole(false);
        role.setStatus(RoleStatus.ACTIVE);
        role.setMaxDiscountPct(request.getMaxDiscountPct());
        role.setProviderRole(request.isProviderRole());
        Role saved = roleRepository.save(role);
        if (request.getPermissionIds() != null && !request.getPermissionIds().isEmpty()) {
            rolePermissionRepository.setPermissionsForRole(saved.getId(), request.getPermissionIds());
        }
        return toRoleResponse(saved, groupNamesMap(), permById);
    }

    @Transactional
    @CacheEvict(cacheNames = "staffRoleCatalog", allEntries = true)
    public RoleResponse updateRole(UUID roleId, UpdateRoleRequest request, UUID currentUserId) {
        Role role = roleRepository.findById(roleId)
                .orElseThrow(() -> new BusinessException("Role not found"));
        boolean any = false;
        if (request.getName() != null) {
            if (request.getName().isBlank()) {
                throw new BusinessException("Name cannot be blank");
            }
            role.setName(request.getName().trim());
            any = true;
        }
        if (request.getDescription() != null) {
            role.setDescription(request.getDescription().trim().isEmpty() ? null : request.getDescription().trim());
            any = true;
        }
        if (request.getNameAr() != null) {
            role.setNameAr(request.getNameAr().trim().isEmpty() ? null : request.getNameAr().trim());
            any = true;
        }
        if (request.getDescriptionAr() != null) {
            role.setDescriptionAr(request.getDescriptionAr().trim().isEmpty() ? null : request.getDescriptionAr().trim());
            any = true;
        }
        if (Boolean.TRUE.equals(request.getRemoveFromGroup())) {
            role.setGroupId(null);
            any = true;
        } else if (request.getGroupId() != null) {
            if (groupRepository.findById(request.getGroupId()).isEmpty()) {
                throw new BusinessException("Group not found: " + request.getGroupId());
            }
            role.setGroupId(request.getGroupId());
            any = true;
        }
        if (request.getMaxDiscountPct() != null) {
            role.setMaxDiscountPct(request.getMaxDiscountPct());
            any = true;
        }
        if (request.getProviderRole() != null) {
            role.setProviderRole(request.getProviderRole());
            any = true;
        }
        if (!any) {
            throw new BusinessException("Provide at least one field to update");
        }
        Role saved = roleRepository.save(role);
        auditLogService.logAction("Role metadata updated", ROLE_ENTITY, roleId.toString(), currentUserId,
                "code=" + role.getCode(), null, null, null);
        return toRoleResponse(saved, groupNamesMap(), permissionMap());
    }

    @Transactional
    @CacheEvict(cacheNames = {"staffRoleCatalog", "userPermissions"}, allEntries = true)
    public RoleResponse updateRoleNavigation(UUID roleId, UpdateRoleNavigationRequest request, UUID currentUserId) {
        CompanySidebarCatalog.assertAllRequestedIdsKnown(request.getVisibleItemIds());
        Role role = roleRepository.findById(roleId)
                .orElseThrow(() -> new BusinessException("Role not found"));
        List<String> sanitized = CompanySidebarCatalog.sanitizeVisibleItemIds(request.getVisibleItemIds());
        // Store empty list (not null) so NavigationService can distinguish "explicitly cleared" from "never customized".
        role.setNavigationItemIds(sanitized);
        Role saved = roleRepository.save(role);
        String kind = role.isSystemRole() ? "system" : "custom";
        auditLogService.logAction("Role navigation updated (" + kind + ")", ROLE_ENTITY, roleId.toString(), currentUserId,
                "items=" + sanitized.size(), null, null, null);
        return toRoleResponse(saved, groupNamesMap(), permissionMap());
    }

    @Transactional
    @CacheEvict(cacheNames = {"staffRoleCatalog", "userPermissions"}, allEntries = true)
    public RoleResponse updateRolePermissions(UUID roleId, UpdateRolePermissionsRequest request, UUID currentUserId) {
        Role role = roleRepository.findById(roleId)
                .orElseThrow(() -> new NoSuchElementException("Role not found: " + roleId));
        Map<UUID, Permission> permById = permissionMap();
        List<UUID> ids = request.getPermissionIds() != null ? request.getPermissionIds() : List.of();
        if (!ids.isEmpty()) {
            validateAllPermissionsExist(ids, permById.keySet());
        }
        rolePermissionRepository.setPermissionsForRole(roleId, ids);
        String kind = role.isSystemRole() ? "system" : "custom";
        auditLogService.logAction("Role permissions updated (" + kind + ")", ROLE_ENTITY, roleId.toString(), currentUserId,
                "count=" + ids.size(), null, null, null);
        return toRoleResponse(roleRepository.findById(roleId).orElse(role), groupNamesMap(), permById);
    }

    @Transactional
    @CacheEvict(cacheNames = {"staffRoleCatalog", "userPermissions"}, allEntries = true)
    public void deleteRole(UUID roleId, DeleteRoleRequest request, UUID currentUserId) {
        Role role = roleRepository.findById(roleId)
                .orElseThrow(() -> new NoSuchElementException("Role not found: " + roleId));
        long userCount = userRoleAssignmentRepository.countByRoleId(roleId);
        if (userCount > 0) {
            if (request.getTargetRoleId() == null) {
                throw new IllegalArgumentException("This role has " + userCount + " user(s) assigned. Provide targetRoleId to reassign them before deletion.");
            }
            Role target = roleRepository.findById(request.getTargetRoleId())
                    .orElseThrow(() -> new NoSuchElementException("Target role not found: " + request.getTargetRoleId()));
            if (target.getId().equals(roleId)) {
                throw new IllegalArgumentException("Target role must be different from the role being deleted.");
            }
            userRoleAssignmentRepository.reassignAllToRole(roleId, request.getTargetRoleId());
        }
        rolePermissionRepository.deleteByRoleId(roleId);
        String roleCode = role.getCode();
        roleRepository.deleteById(roleId);
        String kind = role.isSystemRole() ? "system" : "custom";
        auditLogService.logAction("Role Deleted (" + kind + ")", ROLE_ENTITY, roleId.toString(), currentUserId,
                "code=" + roleCode, null, null, null);
    }

    // ── Private helpers ──────────────────────────────────────────────────────────

    private UUID resolveGroupForNewRole(CreateRoleRequest request, UUID currentUserId) {
        if (request.getGroupId() != null) {
            if (groupRepository.findById(request.getGroupId()).isEmpty()) {
                throw new BusinessException("Group not found");
            }
            return request.getGroupId();
        }
        String desiredName = GroupManagementService.trimToNull(request.getCreateGroupName());
        if (desiredName == null) {
            desiredName = request.getName().trim() + " Group";
        }
        Group g = new Group();
        g.setName(desiredName);
        g.setCode(groupManagementService.allocateNextCustomGroupCode());
        g.setDescription("Auto-created with role: " + request.getName().trim());
        Group saved = groupRepository.save(g);
        auditLogService.logAction("Group auto-created for role", "groups", saved.getId().toString(), currentUserId,
                "code=" + saved.getCode(), null, null, null);
        return saved.getId();
    }

    private void validateAllPermissionsExist(List<UUID> permissionIds, Set<UUID> knownIds) {
        for (UUID id : permissionIds) {
            if (!knownIds.contains(id)) {
                throw new IllegalArgumentException("Unknown permission id: " + id);
            }
        }
    }

    private void validateNoLockedPermissions(List<UUID> permissionIds, Map<UUID, Permission> permById) {
        for (UUID id : permissionIds) {
            Permission p = permById.get(id);
            if (p != null && p.isLocked()) {
                throw new IllegalArgumentException("Permission " + p.getCode() + " is locked and cannot be assigned to custom roles.");
            }
        }
    }

    private String generateCustomRoleCode(String name) {
        String slug = name.trim().toUpperCase(Locale.ROOT).replaceAll("[^A-Z0-9]", "_").replaceAll("_+", "_");
        if (slug.isEmpty()) slug = "CUSTOM";
        else if (!slug.startsWith("CUSTOM_")) slug = "CUSTOM_" + slug;
        return slug.length() > 30 ? slug.substring(0, 30) : slug;
    }

    private Map<UUID, String> groupNamesMap() {
        return groupRepository.findAll().stream()
                .collect(Collectors.toMap(Group::getId, g -> RequestLocaleHolder.localized(g.getName(), g.getNameAr()), (a, b) -> a));
    }

    private Map<UUID, Permission> permissionMap() {
        return permissionRepository.findAll().stream()
                .collect(Collectors.toMap(Permission::getId, p -> p, (a, b) -> a));
    }

    private RoleResponse toRoleResponse(Role r, Map<UUID, String> groupNames, Map<UUID, Permission> permById) {
        return toRoleResponse(r, groupNames, permById, null);
    }

    private RoleResponse toRoleResponse(Role r, Map<UUID, String> groupNames, Map<UUID, Permission> permById,
                                        Map<UUID, Set<UUID>> preloadedPermIds) {
        List<UUID> permIds = preloadedPermIds != null
                ? List.copyOf(preloadedPermIds.getOrDefault(r.getId(), Set.of()))
                : rolePermissionRepository.findPermissionIdsByRoleId(r.getId());
        List<PermissionSummaryResponse> perms = permIds.stream()
                .map(permById::get)
                .filter(Objects::nonNull)
                .map(permissionQueryService::toPermissionSummary)
                .collect(Collectors.toList());
        long userCount = userRoleAssignmentRepository.countByRoleId(r.getId());
        // For system roles never customized (null), return the default nav so the editor shows their effective sidebar.
        List<String> navIds = r.getNavigationItemIds();
        if (navIds == null && r.isSystemRole() && r.getCode() != null) {
            try {
                navIds = CompanySidebarCatalog.defaultVisibleItemIdsForUserRole(
                        com.ziyara.backend.domain.enums.UserRole.valueOf(r.getCode()));
            } catch (IllegalArgumentException ignored) {
                navIds = List.of();
            }
        }
        return RoleResponse.builder()
                .id(r.getId())
                .name(RequestLocaleHolder.localized(r.getName(), r.getNameAr()))
                .code(r.getCode())
                .description(RequestLocaleHolder.localized(r.getDescription(), r.getDescriptionAr()))
                .level(r.getLevel())
                .groupId(r.getGroupId())
                .groupName(r.getGroupId() != null ? groupNames.get(r.getGroupId()) : null)
                .systemRole(r.isSystemRole())
                .status(r.getStatus() != null ? r.getStatus() : RoleStatus.ACTIVE)
                .permissionIds(permIds)
                .permissions(perms)
                .userCount(userCount)
                .navigationItemIds(navIds)
                .maxDiscountPct(r.getMaxDiscountPct())
                .providerRole(r.isProviderRole())
                .build();
    }
}
