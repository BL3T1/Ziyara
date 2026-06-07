package com.ziyara.backend.application.service;

import com.ziyara.backend.application.dto.response.UserNavigationResponse;
import com.ziyara.backend.application.exception.BusinessException;
import com.ziyara.backend.domain.entity.Role;
import com.ziyara.backend.domain.entity.User;
import com.ziyara.backend.domain.enums.UserRole;
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
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class NavigationServiceTest {

    @Mock UserRepository userRepository;
    @Mock UserRoleAssignmentRepository userRoleAssignmentRepository;
    @Mock RoleRepository roleRepository;

    NavigationService service;

    @BeforeEach
    void setUp() {
        service = new NavigationService(userRepository, userRoleAssignmentRepository, roleRepository);
    }

    @Test
    void resolveNavigationForUser_userNotFound_throwsBusinessException() {
        UUID userId = UUID.randomUUID();
        when(userRepository.findById(userId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.resolveNavigationForUser(userId))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("User not found");
    }

    @Test
    void resolveNavigationForUser_noRbacAssignment_returnsDefaultNavigation() {
        UUID userId = UUID.randomUUID();
        User user = new User();
        user.setId(userId);
        user.setRole(UserRole.COMPANY_STAFF);

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(userRoleAssignmentRepository.findNewestRoleIdForUser(userId)).thenReturn(Optional.empty());

        UserNavigationResponse result = service.resolveNavigationForUser(userId);

        assertThat(result.getSource()).isEqualTo("default_user_role");
        assertThat(result.getUserRole()).isEqualTo("COMPANY_STAFF");
        assertThat(result.getVisibleItemIds()).isNotNull();
    }

    @Test
    void resolveNavigationForUser_rbacRoleWithNullNavItems_fallsBackToDefault() {
        UUID userId = UUID.randomUUID();
        UUID roleId = UUID.randomUUID();
        User user = new User();
        user.setId(userId);
        user.setRole(UserRole.COMPANY_STAFF);

        Role rbacRole = new Role();
        rbacRole.setId(roleId);
        rbacRole.setCode("CUSTOM_ROLE");
        rbacRole.setNavigationItemIds(null);

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(userRoleAssignmentRepository.findNewestRoleIdForUser(userId)).thenReturn(Optional.of(roleId));
        when(roleRepository.findById(roleId)).thenReturn(Optional.of(rbacRole));

        UserNavigationResponse result = service.resolveNavigationForUser(userId);

        assertThat(result.getSource()).isEqualTo("default_user_role");
    }

    @Test
    void resolveNavigationForUser_rbacRoleWithNavItems_returnsRbacNav() {
        UUID userId = UUID.randomUUID();
        UUID roleId = UUID.randomUUID();
        User user = new User();
        user.setId(userId);
        user.setRole(UserRole.COMPANY_STAFF);

        Role rbacRole = new Role();
        rbacRole.setId(roleId);
        rbacRole.setCode("CUSTOM_ROLE");
        rbacRole.setNavigationItemIds(List.of("dashboard", "bookings"));

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(userRoleAssignmentRepository.findNewestRoleIdForUser(userId)).thenReturn(Optional.of(roleId));
        when(roleRepository.findById(roleId)).thenReturn(Optional.of(rbacRole));

        UserNavigationResponse result = service.resolveNavigationForUser(userId);

        assertThat(result.getSource()).isEqualTo("rbac_role");
        assertThat(result.getRbacRoleId()).isEqualTo(roleId);
    }

    @Test
    void resolveNavigationForUser_rbacRoleNotFound_fallsBackToDefault() {
        UUID userId = UUID.randomUUID();
        UUID roleId = UUID.randomUUID();
        User user = new User();
        user.setId(userId);
        user.setRole(UserRole.COMPANY_STAFF);

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(userRoleAssignmentRepository.findNewestRoleIdForUser(userId)).thenReturn(Optional.of(roleId));
        when(roleRepository.findById(roleId)).thenReturn(Optional.empty());

        UserNavigationResponse result = service.resolveNavigationForUser(userId);

        assertThat(result.getSource()).isEqualTo("default_user_role");
    }
}
