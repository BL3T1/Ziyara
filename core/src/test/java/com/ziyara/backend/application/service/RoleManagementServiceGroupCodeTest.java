package com.ziyara.backend.application.service;

import com.ziyara.backend.application.dto.request.CreateGroupRequest;
import com.ziyara.backend.application.dto.request.UpdateGroupRequest;
import com.ziyara.backend.application.query.GroupMembersQueryHandler;
import com.ziyara.backend.domain.entity.Group;
import com.ziyara.backend.domain.repository.GroupRepository;
import com.ziyara.backend.domain.repository.PermissionRepository;
import com.ziyara.backend.domain.repository.RolePermissionRepository;
import com.ziyara.backend.domain.repository.RoleRepository;
import com.ziyara.backend.domain.repository.UserRepository;
import com.ziyara.backend.domain.repository.UserRoleAssignmentRepository;
import com.ziyara.backend.modules.sys.api.AuditServiceApi;
import com.ziyara.backend.presentation.exception.BusinessException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RoleManagementServiceGroupCodeTest {

    @Mock RoleRepository roleRepository;
    @Mock PermissionRepository permissionRepository;
    @Mock GroupRepository groupRepository;
    @Mock RolePermissionRepository rolePermissionRepository;
    @Mock UserRoleAssignmentRepository userRoleAssignmentRepository;
    @Mock UserRepository userRepository;
    @Mock AuditServiceApi auditLogService;
    @Mock GroupMembersQueryHandler groupMembersQueryHandler;

    @InjectMocks RoleManagementService roleManagementService;

    @Test
    void createGroup_rejectsReservedPlatformZCode() {
        CreateGroupRequest req = new CreateGroupRequest();
        req.setName("Rogue");
        req.setCode("Z9");
        when(groupRepository.existsByCode("Z9")).thenReturn(false);

        assertThrows(BusinessException.class,
                () -> roleManagementService.createGroup(req, UUID.randomUUID()));
    }

    @Test
    void createGroup_rejectsLowercaseZAfterNormalize() {
        CreateGroupRequest req = new CreateGroupRequest();
        req.setName("Rogue");
        req.setCode("z3");
        when(groupRepository.existsByCode("Z3")).thenReturn(false);

        assertThrows(BusinessException.class,
                () -> roleManagementService.createGroup(req, UUID.randomUUID()));
    }

    @Test
    void createGroup_autoAssignsFirstCustomCodeC1() {
        CreateGroupRequest req = new CreateGroupRequest();
        req.setName("Regional Ops");
        req.setCode(null);

        when(groupRepository.findAll()).thenReturn(List.of());
        when(groupRepository.existsByCode("C1")).thenReturn(false);
        when(groupRepository.save(any(Group.class))).thenAnswer(inv -> {
            Group g = inv.getArgument(0);
            if (g.getId() == null) {
                g.setId(UUID.randomUUID());
            }
            return g;
        });

        roleManagementService.createGroup(req, UUID.randomUUID());

        verify(groupRepository).save(argThat((Group g) -> "C1".equals(g.getCode())));
    }

    @Test
    void createGroup_autoAssignsNextAfterExistingC() {
        CreateGroupRequest req = new CreateGroupRequest();
        req.setName("Another");
        req.setCode(null);

        Group existing = new Group();
        existing.setCode("C2");
        when(groupRepository.findAll()).thenReturn(new ArrayList<>(List.of(existing)));
        when(groupRepository.existsByCode("C3")).thenReturn(false);
        when(groupRepository.save(any(Group.class))).thenAnswer(inv -> {
            Group g = inv.getArgument(0);
            if (g.getId() == null) {
                g.setId(UUID.randomUUID());
            }
            return g;
        });

        roleManagementService.createGroup(req, UUID.randomUUID());

        verify(groupRepository).save(argThat((Group g) -> "C3".equals(g.getCode())));
    }

    @Test
    void updateGroup_platformSlice_rejectsCodeChange() {
        UUID z1 = UUID.fromString("b0000000-0000-0000-0000-000000000001");
        Group g = new Group();
        g.setId(z1);
        g.setName("Leadership");
        g.setCode("Z1");
        when(groupRepository.findById(z1)).thenReturn(Optional.of(g));

        UpdateGroupRequest req = UpdateGroupRequest.builder().code("Z9").build();

        assertThrows(BusinessException.class,
                () -> roleManagementService.updateGroup(z1, req, UUID.randomUUID()));
    }

    @Test
    void deleteGroup_platformSlice_rejected() {
        UUID z1 = UUID.fromString("b0000000-0000-0000-0000-000000000001");
        Group g = new Group();
        g.setId(z1);
        when(groupRepository.findById(z1)).thenReturn(Optional.of(g));

        assertThrows(BusinessException.class,
                () -> roleManagementService.deleteGroup(z1, UUID.randomUUID()));
        verify(groupRepository).findById(z1);
        verifyNoMoreInteractions(groupRepository);
    }

    @Test
    void deleteGroup_custom_deletesWhenNoRolesOrAssignments() {
        UUID gid = UUID.fromString("c0000000-0000-0000-0000-000000000099");
        Group g = new Group();
        g.setId(gid);
        g.setCode("C99");
        when(groupRepository.findById(gid)).thenReturn(Optional.of(g));
        when(roleRepository.findByGroupIdOrderByName(gid)).thenReturn(List.of());
        when(userRoleAssignmentRepository.countByGroupId(gid)).thenReturn(0L);

        roleManagementService.deleteGroup(gid, UUID.randomUUID());

        verify(groupRepository).deleteById(gid);
    }
}
