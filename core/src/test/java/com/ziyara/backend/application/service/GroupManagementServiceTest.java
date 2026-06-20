package com.ziyara.backend.application.service;

import com.ziyara.backend.application.dto.request.CreateGroupRequest;
import com.ziyara.backend.application.dto.response.GroupResponse;
import com.ziyara.backend.application.exception.BusinessException;
import com.ziyara.backend.application.query.GroupMembersQueryHandler;
import com.ziyara.backend.domain.entity.Group;
import com.ziyara.backend.domain.repository.GroupRepository;
import com.ziyara.backend.domain.repository.RoleRepository;
import com.ziyara.backend.domain.repository.UserRepository;
import com.ziyara.backend.domain.repository.UserRoleAssignmentRepository;
import com.ziyara.backend.modules.sys.api.AuditServiceApi;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class GroupManagementServiceTest {

    @Mock GroupRepository groupRepository;
    @Mock RoleRepository roleRepository;
    @Mock UserRoleAssignmentRepository userRoleAssignmentRepository;
    @Mock UserRepository userRepository;
    @Mock AuditServiceApi auditLogService;
    @Mock GroupMembersQueryHandler groupMembersQueryHandler;

    GroupManagementService service;

    @BeforeEach
    void setUp() {
        service = new GroupManagementService(
                groupRepository, roleRepository,
                userRoleAssignmentRepository, userRepository,
                auditLogService, groupMembersQueryHandler);
    }

    // ── getGroups ─────────────────────────────────────────────────────────────

    @Test
    void getGroups_returnsAllGroups() {
        Group g = new Group();
        g.setId(UUID.randomUUID());
        g.setName("Finance");
        g.setCode("C1");
        when(groupRepository.findAll()).thenReturn(List.of(g));

        List<GroupResponse> result = service.getGroups();

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getCode()).isEqualTo("C1");
    }

    @Test
    void getGroups_emptyRepository_returnsEmpty() {
        when(groupRepository.findAll()).thenReturn(List.of());

        List<GroupResponse> result = service.getGroups();

        assertThat(result).isEmpty();
    }

    // ── createGroup ───────────────────────────────────────────────────────────

    @Test
    void createGroup_blankName_throwsBusinessException() {
        CreateGroupRequest request = new CreateGroupRequest();
        request.setName("   ");

        assertThatThrownBy(() -> service.createGroup(request, UUID.randomUUID()))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Name cannot be blank");
    }

    @Test
    void createGroup_duplicateCode_throwsIllegalArgument() {
        CreateGroupRequest request = new CreateGroupRequest();
        request.setName("New Group");
        request.setCode("MYCODE");
        when(groupRepository.existsByCode("MYCODE")).thenReturn(true);

        assertThatThrownBy(() -> service.createGroup(request, UUID.randomUUID()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("MYCODE");
    }

    @Test
    void createGroup_invalidCodeFormat_throwsIllegalArgument() {
        CreateGroupRequest request = new CreateGroupRequest();
        request.setName("New Group");
        request.setCode("invalid-code!");

        assertThatThrownBy(() -> service.createGroup(request, UUID.randomUUID()))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void createGroup_validRequest_savesAndReturnsGroup() {
        UUID userId = UUID.randomUUID();
        CreateGroupRequest request = new CreateGroupRequest();
        request.setName("Finance Team");
        request.setCode("FINANCE");

        when(groupRepository.existsByCode("FINANCE")).thenReturn(false);

        Group saved = new Group();
        saved.setId(UUID.randomUUID());
        saved.setName("Finance Team");
        saved.setCode("FINANCE");
        when(groupRepository.save(any())).thenReturn(saved);

        GroupResponse result = service.createGroup(request, userId);

        assertThat(result.getCode()).isEqualTo("FINANCE");
        verify(auditLogService).logAction(eq("Group created"), any(), any(), eq(userId), any(), any(), any(), any());
    }

    @Test
    void createGroup_nullCode_autoAllocatesCode() {
        UUID userId = UUID.randomUUID();
        CreateGroupRequest request = new CreateGroupRequest();
        request.setName("Auto Code Group");

        when(groupRepository.findAll()).thenReturn(List.of());
        when(groupRepository.existsByCode(any())).thenReturn(false);

        Group saved = new Group();
        saved.setId(UUID.randomUUID());
        saved.setName("Auto Code Group");
        saved.setCode("C1");
        when(groupRepository.save(any())).thenReturn(saved);

        GroupResponse result = service.createGroup(request, userId);

        ArgumentCaptor<Group> captor = ArgumentCaptor.forClass(Group.class);
        verify(groupRepository).save(captor.capture());
        assertThat(captor.getValue().getName()).isEqualTo("Auto Code Group");
    }
}
