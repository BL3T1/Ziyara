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
import com.ziyara.backend.application.query.GroupMembersQueryHandler;
import com.ziyara.backend.application.dto.response.RoleResponse;
import com.ziyara.backend.application.locale.RequestLocaleHolder;
import com.ziyara.backend.domain.entity.Group;
import com.ziyara.backend.domain.entity.Permission;
import com.ziyara.backend.domain.entity.Role;
import com.ziyara.backend.domain.catalog.PlatformOrgGroups;
import com.ziyara.backend.domain.enums.RoleLevel;
import com.ziyara.backend.domain.enums.RoleStatus;
import com.ziyara.backend.domain.enums.UserRole;
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
 * Role Management (ROLE_MANAGEMENT_REPORT). Super Admin only. Implements RoleServiceApi (Phase 3).
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
    private final GroupMembersQueryHandler groupMembersQueryHandler;

    private static final String ROLE_ENTITY = "roles";
    private static final String GROUP_ENTITY = "groups";
    /** Synthetic group for roles with no group_id (API-only, not stored in sys_groups). */
    private static final java.util.UUID UNGROUPED_SUMMARY_ID =
            java.util.UUID.fromString("00000000-0000-4000-8000-0000000000ff");

    @Transactional(readOnly = true)
    public List<RoleResponse> listRoles() {
        List<Role> roles = roleRepository.findAllOrderByName();
        Map<UUID, String> groupNames = groupRepository.findAll().stream()
                .collect(Collectors.toMap(Group::getId, g -> RequestLocaleHolder.localized(g.getName(), g.getNameAr()), (a, b) -> a));
        Map<UUID, Permission> permById = permissionMap();
        return roles.stream()
                .map(r -> toRoleResponse(r, groupNames, permById))
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public Optional<RoleResponse> getRole(UUID id) {
        Map<UUID, Permission> permById = permissionMap();
        return roleRepository.findById(id)
                .map(r -> toRoleResponse(r, groupNamesMap(), permById));
    }

    @Transactional(readOnly = true)
    @Cacheable(cacheNames = "permissionCatalogue", key = "'all'")
    public List<PermissionSummaryResponse> getPermissionCatalogue() {
        return permissionRepository.findAll().stream()
                .map(this::toPermissionSummary)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<PermissionSummaryResponse> getUnlockedPermissions() {
        return permissionRepository.findAllUnlocked().stream()
                .map(this::toPermissionSummary)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<GroupResponse> getGroups() {
        return groupRepository.findAll().stream()
                .map(g -> GroupResponse.builder()
                        .id(g.getId())
                        .name(RequestLocaleHolder.localized(g.getName(), g.getNameAr()))
                        .code(g.getCode())
                        .description(RequestLocaleHolder.localized(g.getDescription(), g.getDescriptionAr()))
                        .build())
                .collect(Collectors.toList());
    }

    @Transactional
    @CacheEvict(cacheNames = "staffRoleCatalog", allEntries = true)
    public GroupResponse createGroup(CreateGroupRequest request, UUID currentUserId) {
        String name = request.getName().trim();
        if (name.isEmpty()) {
            throw new BusinessException("Name cannot be blank");
        }
        String codeInput = trimToNull(request.getCode());
        final String code;
        if (codeInput == null) {
            code = allocateNextCustomGroupCode();
        } else {
            String c = codeInput.toUpperCase(Locale.ROOT);
            if (c.length() > 20) {
                throw new BusinessException("Code must be at most 20 characters");
            }
            if (!c.matches("[A-Z0-9_]+")) {
                throw new IllegalArgumentException("Code must contain only letters, digits, or underscore");
            }
            if (groupRepository.existsByCode(c)) {
                throw new IllegalArgumentException("A group with code " + c + " already exists");
            }
            if (PlatformOrgGroups.isReservedPlatformGroupCode(c)) {
                throw new BusinessException(
                        "Reserved platform group codes (Z followed by digits) cannot be created via API.");
            }
            code = c;
        }
        Group g = new Group();
        g.setName(name);
        g.setNameAr(trimToNull(request.getNameAr()));
        g.setCode(code);
        g.setDescription(trimToNull(request.getDescription()));
        g.setDescriptionAr(trimToNull(request.getDescriptionAr()));
        Group saved = groupRepository.save(g);
        auditLogService.logAction("Group created", GROUP_ENTITY, saved.getId().toString(), currentUserId,
                "code=" + code, null, null, null);
        return GroupResponse.builder()
                .id(saved.getId())
                .name(RequestLocaleHolder.localized(saved.getName(), saved.getNameAr()))
                .code(saved.getCode())
                .description(RequestLocaleHolder.localized(saved.getDescription(), saved.getDescriptionAr()))
                .build();
    }

    @Transactional
    @CacheEvict(cacheNames = "staffRoleCatalog", allEntries = true)
    public GroupResponse updateGroup(UUID groupId, UpdateGroupRequest request, UUID currentUserId) {
        Group group = groupRepository.findById(groupId)
                .orElseThrow(() -> new BusinessException("Group not found"));
        boolean platform = PlatformOrgGroups.isPlatformGroupId(groupId);
        boolean any = false;
        if (request.getName() != null) {
            if (request.getName().isBlank()) {
                throw new BusinessException("Name cannot be blank");
            }
            group.setName(request.getName().trim());
            any = true;
        }
        if (request.getNameAr() != null) {
            group.setNameAr(request.getNameAr().trim().isEmpty() ? null : request.getNameAr().trim());
            any = true;
        }
        if (request.getDescription() != null) {
            group.setDescription(request.getDescription().trim().isEmpty() ? null : request.getDescription().trim());
            any = true;
        }
        if (request.getDescriptionAr() != null) {
            group.setDescriptionAr(
                    request.getDescriptionAr().trim().isEmpty() ? null : request.getDescriptionAr().trim());
            any = true;
        }
        if (request.getCode() != null) {
            String normalized = request.getCode().trim().toUpperCase(Locale.ROOT);
            if (normalized.isEmpty()) {
                throw new BusinessException("Code cannot be blank when provided");
            }
            if (platform && !normalized.equals(group.getCode())) {
                throw new BusinessException("Platform group code cannot be changed");
            }
            if (!normalized.equals(group.getCode())) {
                if (normalized.length() > 20) {
                    throw new BusinessException("Code must be at most 20 characters");
                }
                if (!normalized.matches("[A-Z0-9_]+")) {
                    throw new IllegalArgumentException("Code must contain only letters, digits, or underscore");
                }
                if (PlatformOrgGroups.isReservedPlatformGroupCode(normalized)) {
                    throw new BusinessException(
                            "Reserved platform group codes (Z followed by digits) cannot be assigned to a group.");
                }
                if (groupRepository.existsByCodeAndIdNot(normalized, groupId)) {
                    throw new IllegalArgumentException("A group with code " + normalized + " already exists");
                }
                group.setCode(normalized);
                any = true;
            }
        }
        if (!any) {
            throw new BusinessException("Provide at least one of name, nameAr, description, descriptionAr, or code");
        }
        Group saved = groupRepository.save(group);
        auditLogService.logAction("Group updated", GROUP_ENTITY, saved.getId().toString(), currentUserId,
                "code=" + saved.getCode(), null, null, null);
        return GroupResponse.builder()
                .id(saved.getId())
                .name(RequestLocaleHolder.localized(saved.getName(), saved.getNameAr()))
                .code(saved.getCode())
                .description(RequestLocaleHolder.localized(saved.getDescription(), saved.getDescriptionAr()))
                .build();
    }

    @Transactional
    @CacheEvict(cacheNames = "staffRoleCatalog", allEntries = true)
    public void deleteGroup(UUID groupId, UUID currentUserId) {
        if (groupRepository.findById(groupId).isEmpty()) {
            throw new BusinessException("Group not found");
        }
        if (PlatformOrgGroups.isPlatformGroupId(groupId)) {
            throw new BusinessException("Cannot delete a platform organizational group");
        }
        if (!roleRepository.findByGroupIdOrderByName(groupId).isEmpty()) {
            throw new BusinessException("Cannot delete group while roles are assigned to it");
        }
        if (userRoleAssignmentRepository.countByGroupId(groupId) > 0) {
            throw new BusinessException("Cannot delete group while user role assignments still reference it");
        }
        groupRepository.deleteById(groupId);
        auditLogService.logAction("Group deleted", GROUP_ENTITY, groupId.toString(), currentUserId,
                null, null, null, null);
    }

    /**
     * Next free {@code C}{n} code for admin-created organizational groups (never {@code Z}+digits â€” reserved for platform).
     */
    private String allocateNextCustomGroupCode() {
        int max = 0;
        for (Group group : groupRepository.findAll()) {
            String c = group.getCode();
            if (c == null || c.length() < 2) {
                continue;
            }
            char first = Character.toUpperCase(c.charAt(0));
            if (first != 'C') {
                continue;
            }
            try {
                int n = Integer.parseInt(c.substring(1));
                if (n > max) {
                    max = n;
                }
            } catch (NumberFormatException ignored) {
                // skip non-numeric suffix
            }
        }
        for (int n = max + 1; n < max + 10_000; n++) {
            String candidate = "C" + n;
            if (!groupRepository.existsByCode(candidate)) {
                return candidate;
            }
        }
        throw new IllegalStateException("Could not allocate a unique C{n} group code");
    }

    private static String trimToNull(String s) {
        if (s == null) {
            return null;
        }
        String t = s.trim();
        return t.isEmpty() ? null : t;
    }

    @Transactional(readOnly = true)
    public List<GroupSummaryResponse> listGroupSummaries() {
        List<Group> groups = groupRepository.findAll();
        List<Role> roles = roleRepository.findAllOrderByName();
        Map<UUID, List<Role>> rolesByGroup = roles.stream()
                .filter(r -> r.getGroupId() != null)
                .collect(Collectors.groupingBy(Role::getGroupId));
        List<GroupSummaryResponse> rows = groups.stream()
                .sorted(Comparator.comparingInt(RoleManagementService::groupSortOrder))
                .map(g -> {
                    List<Role> inGroup = rolesByGroup.getOrDefault(g.getId(), List.of());
                    int roleCount = inGroup.size();
                    long userCount = inGroup.stream().mapToLong(this::countUsersForRole).sum();
                    return GroupSummaryResponse.builder()
                            .id(g.getId())
                            .name(RequestLocaleHolder.localized(g.getName(), g.getNameAr()))
                            .code(g.getCode())
                            .description(RequestLocaleHolder.localized(g.getDescription(), g.getDescriptionAr()))
                            .roleCount(roleCount)
                            .userCount(userCount)
                            .build();
                })
                .collect(Collectors.toCollection(ArrayList::new));

        List<Role> ungrouped = roles.stream().filter(r -> r.getGroupId() == null).collect(Collectors.toList());
        if (!ungrouped.isEmpty()) {
            long userCount = ungrouped.stream().mapToLong(this::countUsersForRole).sum();
            rows.add(GroupSummaryResponse.builder()
                    .id(UNGROUPED_SUMMARY_ID)
                    .name("Ungrouped roles")
                    .code("UNGROUPED")
                    .description("Roles not linked to an organizational group")
                    .roleCount(ungrouped.size())
                    .userCount(userCount)
                    .build());
        }
        return rows;
    }

    @Override
    @Transactional(readOnly = true)
    public Page<UserResponse> listGroupMembers(UUID groupId, int page, int size) {
        return groupMembersQueryHandler.findUsersInGroup(groupId, page, size);
    }

    private static int groupSortOrder(Group g) {
        String c = g.getCode();
        if (c == null || c.isBlank()) {
            return 999;
        }
        char head = Character.toUpperCase(c.charAt(0));
        if (c.length() >= 2 && (head == 'Z' || head == 'G')) {
            try {
                return Integer.parseInt(c.substring(1));
            } catch (NumberFormatException e) {
                return 998;
            }
        }
        if (c.length() >= 2 && head == 'C') {
            try {
                return 500 + Integer.parseInt(c.substring(1));
            } catch (NumberFormatException e) {
                return 998;
            }
        }
        return 700 + Math.abs(c.hashCode() % 100);
    }

    /**
     * Prefer {@code sys_users.role} when the role code matches {@link UserRole} (realistic for this app);
     * otherwise use {@code sys_user_roles} assignment count.
     */
    private long countUsersForRole(Role r) {
        String code = r.getCode();
        if (code == null || code.isBlank()) {
            return userRoleAssignmentRepository.countByRoleId(r.getId());
        }
        try {
            UserRole ur = UserRole.valueOf(code.trim());
            return userRepository.countByRole(ur);
        } catch (IllegalArgumentException e) {
            return userRoleAssignmentRepository.countByRoleId(r.getId());
        }
    }

    @Transactional
    @CacheEvict(cacheNames = "staffRoleCatalog", allEntries = true)
    public RoleResponse createCustomRole(CreateRoleRequest request, UUID currentUserId) {
        Map<UUID, Permission> permById = permissionMap();
        if (request.getPermissionIds() != null && !request.getPermissionIds().isEmpty()) {
            validateAllPermissionsExist(request.getPermissionIds(), permById.keySet());
            validateNoLockedPermissions(request.getPermissionIds(), permById);
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
        Role saved = roleRepository.save(role);
        if (request.getPermissionIds() != null && !request.getPermissionIds().isEmpty()) {
            rolePermissionRepository.setPermissionsForRole(saved.getId(), request.getPermissionIds());
        }
        return toRoleResponse(saved, groupNamesMap(), permById);
    }

    private UUID resolveGroupForNewRole(CreateRoleRequest request, UUID currentUserId) {
        if (request.getGroupId() != null) {
            if (groupRepository.findById(request.getGroupId()).isEmpty()) {
                throw new BusinessException("Group not found");
            }
            return request.getGroupId();
        }
        String desiredName = trimToNull(request.getCreateGroupName());
        if (desiredName == null) {
            desiredName = request.getName().trim() + " Group";
        }
        Group g = new Group();
        g.setName(desiredName);
        g.setCode(allocateNextCustomGroupCode());
        g.setDescription("Auto-created with role: " + request.getName().trim());
        Group saved = groupRepository.save(g);
        auditLogService.logAction("Group auto-created for role", GROUP_ENTITY, saved.getId().toString(), currentUserId,
                "code=" + saved.getCode(), null, null, null);
        return saved.getId();
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
        if (!any) {
            throw new BusinessException("Provide at least one of name, description, nameAr, or descriptionAr");
        }
        Role saved = roleRepository.save(role);
        auditLogService.logAction("Role metadata updated", ROLE_ENTITY, roleId.toString(), currentUserId,
                "code=" + role.getCode(), null, null, null);
        return toRoleResponse(saved, groupNamesMap(), permissionMap());
    }

    @Transactional
    @CacheEvict(cacheNames = "staffRoleCatalog", allEntries = true)
    public RoleResponse updateRoleNavigation(UUID roleId, UpdateRoleNavigationRequest request, UUID currentUserId) {
        CompanySidebarCatalog.assertAllRequestedIdsKnown(request.getVisibleItemIds());
        Role role = roleRepository.findById(roleId)
                .orElseThrow(() -> new BusinessException("Role not found"));
        List<String> sanitized = CompanySidebarCatalog.sanitizeVisibleItemIds(request.getVisibleItemIds());
        role.setNavigationItemIds(sanitized.isEmpty() ? null : sanitized);
        Role saved = roleRepository.save(role);
        String kind = role.isSystemRole() ? "system" : "custom";
        auditLogService.logAction("Role navigation updated (" + kind + ")", ROLE_ENTITY, roleId.toString(), currentUserId,
                "items=" + sanitized.size(), null, null, null);
        return toRoleResponse(saved, groupNamesMap(), permissionMap());
    }

    @Transactional
    @CacheEvict(cacheNames = "staffRoleCatalog", allEntries = true)
    public RoleResponse updateRolePermissions(UUID roleId, UpdateRolePermissionsRequest request, UUID currentUserId) {
        Role role = roleRepository.findById(roleId)
                .orElseThrow(() -> new NoSuchElementException("Role not found: " + roleId));
        Map<UUID, Permission> permById = permissionMap();
        List<UUID> ids = request.getPermissionIds() != null ? request.getPermissionIds() : List.of();
        if (!ids.isEmpty()) {
            validateAllPermissionsExist(ids, permById.keySet());
            if (!role.isSystemRole()) {
                validateNoLockedPermissions(ids, permById);
            }
        }
        rolePermissionRepository.setPermissionsForRole(roleId, ids);
        String kind = role.isSystemRole() ? "system" : "custom";
        auditLogService.logAction("Role permissions updated (" + kind + ")", ROLE_ENTITY, roleId.toString(), currentUserId,
                "count=" + ids.size(), null, null, null);
        return toRoleResponse(roleRepository.findById(roleId).orElse(role), groupNamesMap(), permById);
    }

    @Transactional
    @CacheEvict(cacheNames = "staffRoleCatalog", allEntries = true)
    public void deleteRole(UUID roleId, DeleteRoleRequest request, UUID currentUserId) {
        Role role = roleRepository.findById(roleId)
                .orElseThrow(() -> new NoSuchElementException("Role not found: " + roleId));
        if (role.isSystemRole()) {
            throw new IllegalStateException("Cannot delete a system role.");
        }
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
        auditLogService.logAction("Role Deleted (with reassignment)", ROLE_ENTITY, roleId.toString(), currentUserId,
                "code=" + roleCode, null, null, null);
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
        List<UUID> permIds = rolePermissionRepository.findPermissionIdsByRoleId(r.getId());
        List<PermissionSummaryResponse> perms = permIds.stream()
                .map(permById::get)
                .filter(Objects::nonNull)
                .map(this::toPermissionSummary)
                .collect(Collectors.toList());
        long userCount = userRoleAssignmentRepository.countByRoleId(r.getId());
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
                .navigationItemIds(r.getNavigationItemIds())
                .build();
    }

    private PermissionSummaryResponse toPermissionSummary(Permission p) {
        return PermissionSummaryResponse.builder()
                .id(p.getId())
                .code(p.getCode())
                .name(RequestLocaleHolder.localized(p.getName(), p.getNameAr()))
                .resource(p.getResource())
                .action(p.getAction())
                .locked(p.isLocked())
                .build();
    }
}
