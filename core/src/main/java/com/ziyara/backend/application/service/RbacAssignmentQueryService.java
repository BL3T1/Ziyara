package com.ziyara.backend.application.service;

import com.ziyara.backend.application.dto.response.RbacRoleOptionResponse;
import com.ziyara.backend.application.dto.response.UserRbacAssignmentResponse;
import com.ziyara.backend.application.locale.RequestLocaleHolder;
import com.ziyara.backend.domain.entity.Role;
import com.ziyara.backend.domain.enums.RoleStatus;
import com.ziyara.backend.domain.repository.RoleRepository;
import com.ziyara.backend.domain.repository.UserRepository;
import com.ziyara.backend.domain.repository.UserRoleAssignmentRepository;
import com.ziyara.backend.presentation.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class RbacAssignmentQueryService {

    private final RoleRepository roleRepository;
    private final UserRepository userRepository;
    private final UserRoleAssignmentRepository userRoleAssignmentRepository;

    @Transactional(readOnly = true)
    public List<RbacRoleOptionResponse> listCustomRolesForAssignment() {
        return roleRepository.findAllOrderByName().stream()
                .filter(r -> !r.isSystemRole() && r.getStatus() == RoleStatus.ACTIVE)
                .map(this::toOption)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public UserRbacAssignmentResponse getUserRbacAssignment(UUID userId) {
        userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException("User not found"));
        return userRoleAssignmentRepository.findNewestRoleIdForUser(userId)
                .flatMap(roleRepository::findById)
                .map(this::toAssignment)
                .orElse(UserRbacAssignmentResponse.builder().build());
    }

    private RbacRoleOptionResponse toOption(Role r) {
        return RbacRoleOptionResponse.builder()
                .id(r.getId())
                .code(r.getCode())
                .name(RequestLocaleHolder.localized(r.getName(), r.getNameAr()))
                .build();
    }

    private UserRbacAssignmentResponse toAssignment(Role r) {
        return UserRbacAssignmentResponse.builder()
                .roleId(r.getId())
                .roleCode(r.getCode())
                .roleName(RequestLocaleHolder.localized(r.getName(), r.getNameAr()))
                .build();
    }
}
