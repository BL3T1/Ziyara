package com.ziyara.backend.application.service;

import com.ziyara.backend.application.dto.response.UserNavigationResponse;
import com.ziyara.backend.application.navigation.CompanySidebarCatalog;
import com.ziyara.backend.domain.entity.Role;
import com.ziyara.backend.domain.entity.User;
import com.ziyara.backend.domain.repository.RoleRepository;
import com.ziyara.backend.domain.repository.UserRepository;
import com.ziyara.backend.domain.repository.UserRoleAssignmentRepository;
import com.ziyara.backend.application.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class NavigationService {

    private final UserRepository userRepository;
    private final UserRoleAssignmentRepository userRoleAssignmentRepository;
    private final RoleRepository roleRepository;

    @Transactional(readOnly = true)
    public UserNavigationResponse resolveNavigationForUser(UUID userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException("User not found"));

        Optional<UUID> assignedRoleId = userRoleAssignmentRepository.findNewestRoleIdForUser(userId);
        if (assignedRoleId.isPresent()) {
            Role rbacRole = roleRepository.findById(assignedRoleId.get()).orElse(null);
            // Non-null (including empty list) means the nav was explicitly set by an admin — respect it exactly.
            // null means never customized — fall through to defaults below.
            if (rbacRole != null && rbacRole.getNavigationItemIds() != null) {
                List<String> ids = CompanySidebarCatalog.sanitizeVisibleItemIds(rbacRole.getNavigationItemIds());
                return UserNavigationResponse.builder()
                        .visibleItemIds(ids)
                        .source("rbac_role")
                        .rbacRoleId(rbacRole.getId())
                        .rbacRoleCode(rbacRole.getCode())
                        .build();
            }
        }

        List<String> defaults = CompanySidebarCatalog.defaultVisibleItemIdsForUserRole(user.getRole());
        return UserNavigationResponse.builder()
                .visibleItemIds(defaults)
                .source("default_user_role")
                .userRole(user.getRole().name())
                .build();
    }
}
