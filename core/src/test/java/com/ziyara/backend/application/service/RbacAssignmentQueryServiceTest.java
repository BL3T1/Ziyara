package com.ziyara.backend.application.service;

import com.ziyara.backend.application.dto.response.RbacRoleOptionResponse;
import com.ziyara.backend.application.dto.response.UserRbacAssignmentResponse;
import com.ziyara.backend.application.exception.BusinessException;
import com.ziyara.backend.domain.entity.Role;
import com.ziyara.backend.domain.enums.RoleStatus;
import com.ziyara.backend.domain.repository.RoleRepository;
import com.ziyara.backend.domain.repository.UserRepository;
import com.ziyara.backend.domain.repository.UserRoleAssignmentRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RbacAssignmentQueryServiceTest {

    @Mock RoleRepository roleRepository;
    @Mock UserRepository userRepository;
    @Mock UserRoleAssignmentRepository userRoleAssignmentRepository;

    RbacAssignmentQueryService service;

    @BeforeEach
    void setUp() {
        service = new RbacAssignmentQueryService(roleRepository, userRepository, userRoleAssignmentRepository);
    }

    // ── listCustomRolesForAssignment ──────────────────────────────────────────

    @Test
    void listCustomRoles_filtersOutSystemRoles() {
        Role customRole = roleWithId("custom", false, RoleStatus.ACTIVE);
        Role systemRole = roleWithId("system", true, RoleStatus.ACTIVE);
        when(roleRepository.findAllOrderByName()).thenReturn(List.of(customRole, systemRole));

        List<RbacRoleOptionResponse> result = service.listCustomRolesForAssignment();

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getCode()).isEqualTo("custom");
    }

    @Test
    void listCustomRoles_filtersOutInactiveRoles() {
        Role activeRole = roleWithId("active-role", false, RoleStatus.ACTIVE);
        Role inactiveRole = roleWithId("inactive-role", false, RoleStatus.INACTIVE);
        when(roleRepository.findAllOrderByName()).thenReturn(List.of(activeRole, inactiveRole));

        List<RbacRoleOptionResponse> result = service.listCustomRolesForAssignment();

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getCode()).isEqualTo("active-role");
    }

    @Test
    void listCustomRoles_emptyRepo_returnsEmpty() {
        when(roleRepository.findAllOrderByName()).thenReturn(List.of());

        assertThat(service.listCustomRolesForAssignment()).isEmpty();
    }

    // ── getUserRbacAssignment ─────────────────────────────────────────────────

    @Test
    void getUserRbacAssignment_userNotFound_throwsBusinessException() {
        UUID userId = UUID.randomUUID();
        when(userRepository.findById(userId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getUserRbacAssignment(userId))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("User not found");
    }

    @Test
    void getUserRbacAssignment_noRoleAssigned_returnsEmptyResponse() {
        UUID userId = UUID.randomUUID();
        when(userRepository.findById(userId)).thenReturn(Optional.of(new com.ziyara.backend.domain.entity.User()));
        when(userRoleAssignmentRepository.findNewestRoleIdForUser(userId)).thenReturn(Optional.empty());

        UserRbacAssignmentResponse result = service.getUserRbacAssignment(userId);

        assertThat(result.getRoleId()).isNull();
    }

    @Test
    void getUserRbacAssignment_withRole_returnsRoleDetails() {
        UUID userId = UUID.randomUUID();
        UUID roleId = UUID.randomUUID();
        Role role = roleWithId("MANAGER", false, RoleStatus.ACTIVE);
        role.setId(roleId);

        when(userRepository.findById(userId)).thenReturn(Optional.of(new com.ziyara.backend.domain.entity.User()));
        when(userRoleAssignmentRepository.findNewestRoleIdForUser(userId)).thenReturn(Optional.of(roleId));
        when(roleRepository.findById(roleId)).thenReturn(Optional.of(role));

        UserRbacAssignmentResponse result = service.getUserRbacAssignment(userId);

        assertThat(result.getRoleId()).isEqualTo(roleId);
        assertThat(result.getRoleCode()).isEqualTo("MANAGER");
    }

    private Role roleWithId(String code, boolean systemRole, RoleStatus status) {
        Role role = new Role();
        role.setId(UUID.randomUUID());
        role.setCode(code);
        role.setName(code);
        role.setSystemRole(systemRole);
        role.setStatus(status);
        return role;
    }
}
