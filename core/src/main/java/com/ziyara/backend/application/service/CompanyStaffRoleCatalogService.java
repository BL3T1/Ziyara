package com.ziyara.backend.application.service;

import com.ziyara.backend.application.dto.response.StaffDirectoryRoleOptionResponse;
import com.ziyara.backend.application.locale.RequestLocaleHolder;
import com.ziyara.backend.domain.entity.Group;
import com.ziyara.backend.domain.entity.Role;
import com.ziyara.backend.domain.enums.RoleLevel;
import com.ziyara.backend.domain.enums.RoleStatus;
import com.ziyara.backend.domain.repository.GroupRepository;
import com.ziyara.backend.domain.repository.RoleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Returns the list of roles the create-user form can assign.
 * All active roles from sys_roles are eligible; only the SUPER_ADMIN
 * system role is excluded (created via seeding only).
 */
@Service
@RequiredArgsConstructor
public class CompanyStaffRoleCatalogService {

    private final RoleRepository roleRepository;
    private final GroupRepository groupRepository;

    @Transactional(readOnly = true)
    public List<StaffDirectoryRoleOptionResponse> listDashboardCreatableRoles() {
        Map<UUID, Group> groupsById = groupRepository.findAll().stream()
                .collect(Collectors.toMap(Group::getId, g -> g, (a, b) -> a));

        return roleRepository.findAllOrderByName().stream()
                .filter(r -> r.getStatus() == RoleStatus.ACTIVE)
                .filter(r -> !(r.isSystemRole() && "SUPER_ADMIN".equals(r.getCode())))
                .map(r -> toOption(r, groupsById))
                .sorted(Comparator.comparing(StaffDirectoryRoleOptionResponse::getDisplayName,
                        String.CASE_INSENSITIVE_ORDER))
                .collect(Collectors.toList());
    }

    public boolean isEligibleForCompanyUserCreationPrimaryRole(Role r) {
        if (r == null || r.getStatus() != RoleStatus.ACTIVE) {
            return false;
        }
        return !(r.isSystemRole() && "SUPER_ADMIN".equals(r.getCode()));
    }

    private StaffDirectoryRoleOptionResponse toOption(Role r, Map<UUID, Group> groupsById) {
        Group g = r.getGroupId() != null ? groupsById.get(r.getGroupId()) : null;
        String source = r.isSystemRole() ? "SYSTEM" : "CUSTOM";
        return StaffDirectoryRoleOptionResponse.builder()
                .source(source)
                .rbacRoleId(r.getId())
                .securityUserRole("STAFF")
                .code(r.getCode())
                .displayName(RequestLocaleHolder.localized(r.getName(), r.getNameAr()))
                .groupId(r.getGroupId())
                .groupName(g != null ? RequestLocaleHolder.localized(g.getName(), g.getNameAr()) : null)
                .groupCode(g != null ? g.getCode() : null)
                .build();
    }
}
