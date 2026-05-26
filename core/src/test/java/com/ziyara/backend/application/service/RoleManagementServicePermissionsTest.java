package com.ziyara.backend.application.service;

import com.ziyara.backend.application.dto.request.UpdateRolePermissionsRequest;
import com.ziyara.backend.application.query.GroupMembersQueryHandler;
import com.ziyara.backend.domain.entity.Permission;
import com.ziyara.backend.domain.entity.Role;
import com.ziyara.backend.domain.repository.*;
import com.ziyara.backend.modules.sys.api.AuditServiceApi;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RoleManagementServicePermissionsTest {

    @Mock RoleRepository roleRepository;
    @Mock PermissionRepository permissionRepository;
    @Mock GroupRepository groupRepository;
    @Mock RolePermissionRepository rolePermissionRepository;
    @Mock UserRoleAssignmentRepository userRoleAssignmentRepository;
    @Mock UserRepository userRepository;
    @Mock AuditServiceApi auditLogService;
    @Mock GroupMembersQueryHandler groupMembersQueryHandler;
    @Mock GroupManagementService groupManagementService;
    @Mock PermissionQueryService permissionQueryService;

    @InjectMocks RoleManagementService service;

    private final UUID roleId = UUID.fromString("c0000000-0000-0000-0000-000000000099");
    private final UUID unlockedId = UUID.fromString("d0000000-0000-0000-0000-000000000001");
    private final UUID lockedId = UUID.fromString("d0000000-0000-0000-0000-0000000000ee");
    private final UUID actorId = UUID.fromString("e0000000-0000-0000-0000-000000000099");

    @BeforeEach
    void stubs() {
        lenient().when(groupRepository.findAll()).thenReturn(List.of());
    }

    @Test
    void updateRolePermissions_systemRole_acceptsLockedPermissionIds() {
        Role role = new Role();
        role.setId(roleId);
        role.setSystemRole(true);
        role.setCode("SUPER_ADMIN");
        when(roleRepository.findById(roleId)).thenReturn(Optional.of(role));

        Permission unlocked = perm(unlockedId, "a:read", false);
        Permission locked = perm(lockedId, "system:super_ops", true);
        when(permissionRepository.findAll()).thenReturn(List.of(unlocked, locked));

        when(rolePermissionRepository.findPermissionIdsByRoleId(roleId)).thenReturn(List.of(unlockedId, lockedId));
        when(userRoleAssignmentRepository.countByRoleId(roleId)).thenReturn(0L);

        UpdateRolePermissionsRequest req = new UpdateRolePermissionsRequest();
        req.setPermissionIds(List.of(unlockedId, lockedId));

        service.updateRolePermissions(roleId, req, actorId);

        verify(rolePermissionRepository).setPermissionsForRole(roleId, List.of(unlockedId, lockedId));
        verify(auditLogService).logAction(contains("Role permissions updated (system)"), eq("roles"), eq(roleId.toString()),
                eq(actorId), any(), isNull(), isNull(), isNull());
    }

    @Test
    void updateRolePermissions_customRole_rejectsLockedPermissionIds() {
        Role role = new Role();
        role.setId(roleId);
        role.setSystemRole(false);
        when(roleRepository.findById(roleId)).thenReturn(Optional.of(role));

        Permission unlocked = perm(unlockedId, "a:read", false);
        Permission locked = perm(lockedId, "system:super_ops", true);
        when(permissionRepository.findAll()).thenReturn(List.of(unlocked, locked));

        UpdateRolePermissionsRequest req = new UpdateRolePermissionsRequest();
        req.setPermissionIds(List.of(unlockedId, lockedId));

        assertThatThrownBy(() -> service.updateRolePermissions(roleId, req, actorId))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("locked");

        verify(rolePermissionRepository, never()).setPermissionsForRole(any(), any());
        verifyNoInteractions(auditLogService);
    }

    @Test
    void updateRolePermissions_rejectsUnknownPermissionId() {
        Role role = new Role();
        role.setId(roleId);
        role.setSystemRole(true);
        when(roleRepository.findById(roleId)).thenReturn(Optional.of(role));

        Permission unlocked = perm(unlockedId, "a:read", false);
        when(permissionRepository.findAll()).thenReturn(List.of(unlocked));

        UUID unknown = UUID.fromString("ffffffff-ffff-ffff-ffff-ffffffffffff");
        UpdateRolePermissionsRequest req = new UpdateRolePermissionsRequest();
        req.setPermissionIds(List.of(unlockedId, unknown));

        assertThatThrownBy(() -> service.updateRolePermissions(roleId, req, actorId))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Unknown permission id");

        verify(rolePermissionRepository, never()).setPermissionsForRole(any(), any());
    }

    private static Permission perm(UUID id, String code, boolean locked) {
        Permission p = new Permission();
        p.setId(id);
        p.setCode(code);
        p.setName(code);
        p.setResource("r");
        p.setAction("a");
        p.setScope("ALL");
        p.setLocked(locked);
        return p;
    }
}
