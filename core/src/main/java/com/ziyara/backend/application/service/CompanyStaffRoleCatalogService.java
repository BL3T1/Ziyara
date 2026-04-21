package com.ziyara.backend.application.service;

import com.ziyara.backend.application.dto.response.StaffDirectoryRoleOptionResponse;
import com.ziyara.backend.application.locale.RequestLocaleHolder;
import com.ziyara.backend.domain.entity.Group;
import com.ziyara.backend.domain.entity.Role;
import com.ziyara.backend.domain.enums.RoleLevel;
import com.ziyara.backend.domain.enums.RoleStatus;
import com.ziyara.backend.domain.enums.UserRole;
import com.ziyara.backend.domain.repository.GroupRepository;
import com.ziyara.backend.domain.repository.RoleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Source for company create-user role picklist: {@link UserRole} rows in {@code sys_roles} plus active custom RBAC roles.
 */
@Service
@RequiredArgsConstructor
public class CompanyStaffRoleCatalogService {

    private final RoleRepository roleRepository;
    private final GroupRepository groupRepository;

    /**
     * Unified options for POST /users: each row includes {@code rbacRoleId} to send as {@code primaryRbacRoleId},
     * or clients may still send {@code role} enum for SYSTEM rows only.
     */
    @Transactional(readOnly = true)
    @Cacheable(cacheNames = "staffRoleCatalog", key = "'dashboard-creatable'")
    public List<StaffDirectoryRoleOptionResponse> listDashboardCreatableRoles() {
        Map<UUID, Group> groupsById = groupRepository.findAll().stream()
                .collect(Collectors.toMap(Group::getId, g -> g, (a, b) -> a));

        List<StaffDirectoryRoleOptionResponse> out = new ArrayList<>();

        for (UserRole ur : UserRole.values()) {
            if (!ur.isCompanyDashboardCreatable()) {
                continue;
            }
            Optional<Role> sysRole = roleRepository.findByCode(ur.name());
            if (sysRole.isEmpty()) {
                continue;
            }
            Role r = sysRole.get();
            out.add(toOption(r, groupsById, "SYSTEM", ur.name(), ur.name()));
        }

        for (Role r : roleRepository.findAllOrderByName()) {
            if (r.isSystemRole()) {
                continue;
            }
            if (r.getStatus() != RoleStatus.ACTIVE) {
                continue;
            }
            if (!isEligibleCustomRoleForCreateUser(r)) {
                continue;
            }
            UserRole security = deriveSecurityUserRoleForCustomRole(r);
            out.add(toOption(r, groupsById, "CUSTOM", security.name(), r.getCode()));
        }

        out.sort(Comparator.comparing(StaffDirectoryRoleOptionResponse::getDisplayName, String.CASE_INSENSITIVE_ORDER));
        return out;
    }

    /**
     * Whether this {@code sys_roles} row may be chosen as primary role when creating company staff.
     */
    public boolean isEligibleForCompanyUserCreationPrimaryRole(Role r) {
        if (r == null || r.getStatus() != RoleStatus.ACTIVE) {
            return false;
        }
        if (r.isSystemRole()) {
            try {
                return UserRole.valueOf(r.getCode()).isCompanyDashboardCreatable();
            } catch (IllegalArgumentException e) {
                return false;
            }
        }
        return isEligibleCustomRoleForCreateUser(r);
    }

    /**
     * {@code sys_users.role} / JWT claim when this {@code sys_roles} row is the user's primary assignment.
     */
    public UserRole resolveSecurityUserRoleForPrimaryRbacRole(Role r) {
        if (r.isSystemRole()) {
            return UserRole.valueOf(r.getCode());
        }
        return deriveSecurityUserRoleForCustomRole(r);
    }

    private boolean isEligibleCustomRoleForCreateUser(Role r) {
        if (r.getLevel() == RoleLevel.SUPER_ADMIN) {
            return false;
        }
        try {
            UserRole parsed = UserRole.valueOf(r.getCode());
            if (!parsed.isCompanyDashboardCreatable()) {
                return false;
            }
            Optional<Role> byCode = roleRepository.findByCode(r.getCode());
            return byCode.filter(Role::isSystemRole).isEmpty();
        } catch (IllegalArgumentException e) {
            return true;
        }
    }

    /**
     * Maps custom RBAC role level to a {@link UserRole} for Spring Security and JWT (coarse-grained {@code hasRole}).
     */
    public UserRole deriveSecurityUserRoleForCustomRole(Role r) {
        RoleLevel level = r.getLevel() != null ? r.getLevel() : RoleLevel.EMPLOYEE;
        return switch (level) {
            case EXECUTIVE -> UserRole.CEO;
            case MANAGER -> UserRole.GENERAL_MANAGER;
            case EMPLOYEE -> UserRole.SALES_REPRESENTATIVE;
            case SUPER_ADMIN -> throw new IllegalArgumentException("Custom role cannot use SUPER_ADMIN level");
        };
    }

    private StaffDirectoryRoleOptionResponse toOption(
            Role r,
            Map<UUID, Group> groupsById,
            String source,
            String securityUserRole,
            String code) {
        Group g = r.getGroupId() != null ? groupsById.get(r.getGroupId()) : null;
        return StaffDirectoryRoleOptionResponse.builder()
                .source(source)
                .rbacRoleId(r.getId())
                .securityUserRole(securityUserRole)
                .code(code)
                .displayName(RequestLocaleHolder.localized(r.getName(), r.getNameAr()))
                .groupId(r.getGroupId())
                .groupName(g != null ? RequestLocaleHolder.localized(g.getName(), g.getNameAr()) : null)
                .groupCode(g != null ? g.getCode() : null)
                .build();
    }
}
