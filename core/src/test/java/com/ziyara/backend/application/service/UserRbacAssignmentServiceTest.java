package com.ziyara.backend.application.service;

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

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserRbacAssignmentServiceTest {

    @Mock UserRepository userRepository;
    @Mock RoleRepository roleRepository;
    @Mock UserRoleAssignmentRepository userRoleAssignmentRepository;

    UserRbacAssignmentService service;

    @BeforeEach
    void setUp() {
        service = new UserRbacAssignmentService(userRepository, roleRepository, userRoleAssignmentRepository);
    }

    // ── assignOrClearPrimaryRbacRole ──────────────────────────────────────────

    @Test
    void assignOrClear_userNotFound_throwsBusinessException() {
        UUID userId = UUID.randomUUID();
        when(userRepository.findById(userId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.assignOrClearPrimaryRbacRole(userId, UUID.randomUUID()))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("User not found");
    }

    @Test
    void assignOrClear_nullRoleId_clearsAssignments() {
        UUID userId = UUID.randomUUID();
        when(userRepository.findById(userId)).thenReturn(Optional.of(new User()));

        service.assignOrClearPrimaryRbacRole(userId, null);

        verify(userRoleAssignmentRepository).clearAssignmentsForUser(userId);
        verify(userRoleAssignmentRepository, never()).setPrimaryRoleForUser(any(), any());
    }

    @Test
    void assignOrClear_roleNotFound_throwsBusinessException() {
        UUID userId = UUID.randomUUID();
        UUID roleId = UUID.randomUUID();
        when(userRepository.findById(userId)).thenReturn(Optional.of(new User()));
        when(roleRepository.findById(roleId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.assignOrClearPrimaryRbacRole(userId, roleId))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Role not found");
    }

    @Test
    void assignOrClear_validUserAndRole_callsSetPrimary() {
        UUID userId = UUID.randomUUID();
        UUID roleId = UUID.randomUUID();
        when(userRepository.findById(userId)).thenReturn(Optional.of(new User()));
        when(roleRepository.findById(roleId)).thenReturn(Optional.of(new Role()));

        service.assignOrClearPrimaryRbacRole(userId, roleId);

        verify(userRoleAssignmentRepository).setPrimaryRoleForUser(userId, roleId);
    }

    // ── autoAssignPrimaryRoleByUserRole ───────────────────────────────────────

    @Test
    void autoAssign_nullUserRole_doesNothing() {
        UUID userId = UUID.randomUUID();

        service.autoAssignPrimaryRoleByUserRole(userId, null);

        verifyNoInteractions(roleRepository, userRoleAssignmentRepository);
    }

    @Test
    void autoAssign_roleCodeFound_assignsPrimary() {
        UUID userId = UUID.randomUUID();
        UUID roleId = UUID.randomUUID();
        Role role = new Role();
        role.setId(roleId);
        when(roleRepository.findByCode("CUSTOMER")).thenReturn(Optional.of(role));

        service.autoAssignPrimaryRoleByUserRole(userId, UserRole.CUSTOMER);

        verify(userRoleAssignmentRepository).setPrimaryRoleForUser(userId, roleId);
    }

    @Test
    void autoAssign_roleCodeNotFound_doesNothing() {
        UUID userId = UUID.randomUUID();
        when(roleRepository.findByCode(any())).thenReturn(Optional.empty());

        service.autoAssignPrimaryRoleByUserRole(userId, UserRole.COMPANY_STAFF);

        verifyNoInteractions(userRoleAssignmentRepository);
    }

    // ── assignPrimaryRoleByRoleId ─────────────────────────────────────────────

    @Test
    void assignByRoleId_nullRoleId_doesNothing() {
        UUID userId = UUID.randomUUID();

        service.assignPrimaryRoleByRoleId(userId, null);

        verifyNoInteractions(roleRepository, userRoleAssignmentRepository);
    }

    @Test
    void assignByRoleId_roleNotFound_throwsBusinessException() {
        UUID userId = UUID.randomUUID();
        UUID roleId = UUID.randomUUID();
        when(roleRepository.findById(roleId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.assignPrimaryRoleByRoleId(userId, roleId))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Role not found");
    }

    @Test
    void assignByRoleId_roleExists_callsSetPrimary() {
        UUID userId = UUID.randomUUID();
        UUID roleId = UUID.randomUUID();
        when(roleRepository.findById(roleId)).thenReturn(Optional.of(new Role()));

        service.assignPrimaryRoleByRoleId(userId, roleId);

        verify(userRoleAssignmentRepository).setPrimaryRoleForUser(userId, roleId);
    }
}
