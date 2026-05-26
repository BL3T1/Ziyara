package com.ziyara.backend.application.service;

import com.ziyara.backend.application.dto.UserResponse;
import com.ziyara.backend.application.dto.request.CreateGroupRequest;
import com.ziyara.backend.application.dto.request.UpdateGroupRequest;
import com.ziyara.backend.application.dto.response.GroupResponse;
import com.ziyara.backend.application.dto.response.GroupSummaryResponse;
import com.ziyara.backend.application.exception.BusinessException;
import com.ziyara.backend.application.locale.RequestLocaleHolder;
import com.ziyara.backend.application.query.GroupMembersQueryHandler;
import com.ziyara.backend.domain.catalog.PlatformOrgGroups;
import com.ziyara.backend.domain.entity.Group;
import com.ziyara.backend.domain.entity.Role;
import com.ziyara.backend.domain.enums.UserRole;
import com.ziyara.backend.domain.repository.GroupRepository;
import com.ziyara.backend.domain.repository.RoleRepository;
import com.ziyara.backend.domain.repository.UserRepository;
import com.ziyara.backend.domain.repository.UserRoleAssignmentRepository;
import com.ziyara.backend.modules.sys.api.AuditServiceApi;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class GroupManagementService {

    private final GroupRepository groupRepository;
    private final RoleRepository roleRepository;
    private final UserRoleAssignmentRepository userRoleAssignmentRepository;
    private final UserRepository userRepository;
    private final AuditServiceApi auditLogService;
    private final GroupMembersQueryHandler groupMembersQueryHandler;

    private static final String GROUP_ENTITY = "groups";
    private static final UUID UNGROUPED_SUMMARY_ID =
            UUID.fromString("00000000-0000-4000-8000-0000000000ff");

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
                        "Reserved platform group codes (C followed by digits) cannot be created via API.");
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
                            "Reserved platform group codes (C followed by digits) cannot be assigned to a group.");
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

    @Transactional(readOnly = true)
    public List<GroupSummaryResponse> listGroupSummaries() {
        List<Group> groups = groupRepository.findAll();
        List<Role> roles = roleRepository.findAllOrderByName();
        Map<UUID, List<Role>> rolesByGroup = roles.stream()
                .filter(r -> r.getGroupId() != null)
                .collect(Collectors.groupingBy(Role::getGroupId));
        List<GroupSummaryResponse> rows = groups.stream()
                .sorted(Comparator.comparingInt(GroupManagementService::groupSortOrder))
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

    @Transactional(readOnly = true)
    public Page<UserResponse> listGroupMembers(UUID groupId, int page, int size) {
        return groupMembersQueryHandler.findUsersInGroup(groupId, page, size);
    }

    public String allocateNextCustomGroupCode() {
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

    static String trimToNull(String s) {
        if (s == null) {
            return null;
        }
        String t = s.trim();
        return t.isEmpty() ? null : t;
    }
}
