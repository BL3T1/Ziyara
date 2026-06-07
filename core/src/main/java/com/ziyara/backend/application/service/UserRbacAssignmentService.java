package com.ziyara.backend.application.service;

import com.ziyara.backend.domain.entity.User;
import com.ziyara.backend.domain.repository.RoleRepository;
import com.ziyara.backend.domain.repository.UserRepository;
import com.ziyara.backend.domain.repository.UserRoleAssignmentRepository;
import com.ziyara.backend.application.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class UserRbacAssignmentService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final UserRoleAssignmentRepository userRoleAssignmentRepository;

    @Transactional
    public void assignOrClearPrimaryRbacRole(UUID targetUserId, UUID roleIdOrNull) {
        userRepository.findById(targetUserId)
                .orElseThrow(() -> new BusinessException("User not found"));
        if (roleIdOrNull == null) {
            userRoleAssignmentRepository.clearAssignmentsForUser(targetUserId);
            return;
        }
        if (!roleRepository.findById(roleIdOrNull).isPresent()) {
            throw new BusinessException("Role not found");
        }
        userRoleAssignmentRepository.setPrimaryRoleForUser(targetUserId, roleIdOrNull);
    }

    /**
     * Sets the user's single primary {@code sys_user_roles} row to the system {@code sys_roles} row whose
     * {@code code} matches {@code userRole}, copying {@code group_id} onto the assignment for group-based summaries.
     * <p>
     * Called after company-dashboard user create and after {@code sys_users.role} is changed by an admin.
     * If an admin had assigned a <em>custom</em> RBAC role for sidebar overrides, that row is replaced â€”
     * they must re-assign the custom role if still required.
     */
    @Transactional
    public void autoAssignPrimaryRoleByUserRole(UUID userId, com.ziyara.backend.domain.enums.UserRole userRole) {
        if (userRole == null) {
            return;
        }
        roleRepository.findByCode(userRole.name())
                .ifPresent(role -> userRoleAssignmentRepository.setPrimaryRoleForUser(userId, role.getId()));
    }

    /**
     * Sets primary {@code sys_user_roles} row to the given {@code sys_roles} id (used after user create with unified picklist).
     */
    @Transactional
    public void assignPrimaryRoleByRoleId(UUID userId, UUID rbacRoleId) {
        if (rbacRoleId == null) {
            return;
        }
        if (roleRepository.findById(rbacRoleId).isEmpty()) {
            throw new BusinessException("Role not found");
        }
        userRoleAssignmentRepository.setPrimaryRoleForUser(userId, rbacRoleId);
    }
}
