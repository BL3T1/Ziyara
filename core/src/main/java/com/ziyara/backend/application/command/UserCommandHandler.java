package com.ziyara.backend.application.command;

import com.ziyara.backend.application.dto.request.CreateUserRequest;
import com.ziyara.backend.application.dto.request.UpdateUserRequest;
import com.ziyara.backend.application.service.CompanyStaffRoleCatalogService;
import com.ziyara.backend.application.service.UserRbacAssignmentService;
import com.ziyara.backend.domain.entity.Role;
import com.ziyara.backend.domain.entity.User;
import com.ziyara.backend.domain.enums.UserRole;
import com.ziyara.backend.domain.enums.UserStatus;
import com.ziyara.backend.domain.repository.RoleRepository;
import com.ziyara.backend.domain.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Locale;
import java.util.UUID;

/**
 * Command handler for user writes (CQRS command side, JPA).
 * Handles create, update, soft-delete, freeze, unfreeze, reset-password.
 */
@Component
@RequiredArgsConstructor
public class UserCommandHandler {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final UserRbacAssignmentService userRbacAssignmentService;
    private final CompanyStaffRoleCatalogService companyStaffRoleCatalogService;

    /**
     * Company dashboard / HR: internal staff via legacy {@code role} enum or unified {@code primaryRbacRoleId}.
     */
    @Transactional
    public User create(CreateUserRequest request) {
        boolean hasEnum = request.getRole() != null;
        boolean hasRbac = request.getPrimaryRbacRoleId() != null;
        if (hasEnum == hasRbac) {
            throw new IllegalArgumentException("Provide exactly one of role or primaryRbacRoleId.");
        }
        UserRole resolvedRole;
        UUID primaryRbacRoleId = null;
        if (hasEnum) {
            if (!request.getRole().isCompanyDashboardCreatable()) {
                throw new IllegalArgumentException(
                        "Choose an internal company role. Super Admin, CUSTOMER, and provider portal roles cannot be created from this form.");
            }
            resolvedRole = request.getRole();
        } else {
            Role rbac = roleRepository.findById(request.getPrimaryRbacRoleId())
                    .orElseThrow(() -> new IllegalArgumentException("Role not found"));
            if (!companyStaffRoleCatalogService.isEligibleForCompanyUserCreationPrimaryRole(rbac)) {
                throw new IllegalArgumentException(
                        "This role cannot be assigned as primary for new company users (inactive, reserved, or not staff-eligible).");
            }
            resolvedRole = companyStaffRoleCatalogService.resolveSecurityUserRoleForPrimaryRbacRole(rbac);
            primaryRbacRoleId = rbac.getId();
        }
        return createInternal(request, resolvedRole, primaryRbacRoleId);
    }

    /**
     * Bootstrap and demo seeding: allows any {@link UserRole} (CUSTOMER, SUPER_ADMIN, provider roles, etc.).
     */
    @Transactional
    public User createForBootstrap(CreateUserRequest request) {
        if (request.getRole() == null) {
            throw new IllegalArgumentException("Bootstrap user creation requires role");
        }
        if (request.getPrimaryRbacRoleId() != null) {
            throw new IllegalArgumentException("Bootstrap user creation does not support primaryRbacRoleId");
        }
        return createInternal(request, request.getRole(), null);
    }

    private User createInternal(CreateUserRequest request, UserRole resolvedRole, UUID primaryRbacRoleIdOverride) {
        String emailNorm = normalizeEmail(request.getEmail());
        if (userRepository.existsByEmail(emailNorm)) {
            throw new IllegalArgumentException("User with email already exists: " + emailNorm);
        }
        if (request.getPhone() != null && !request.getPhone().isBlank() && userRepository.existsByPhone(request.getPhone())) {
            throw new IllegalArgumentException("User with phone already exists");
        }

        User user = new User();
        // Do not set id - let JPA generate it so persist() is used for new users
        user.setEmail(emailNorm);
        user.setPhone(request.getPhone());
        user.setPasswordHash(passwordEncoder.encode(request.getPassword()));
        user.setRole(resolvedRole);
        // Company-created staff: default ACTIVE when status omitted so they can sign in immediately
        String statusRaw = request.getStatus();
        boolean active = statusRaw == null
                || statusRaw.isBlank()
                || "ACTIVE".equalsIgnoreCase(statusRaw.trim());
        user.setStatus(active ? UserStatus.ACTIVE : UserStatus.PENDING_VERIFICATION);
        user.setCreatedAt(null);
        user.setUpdatedAt(null);
        User saved = userRepository.save(user);
        if (primaryRbacRoleIdOverride != null) {
            userRbacAssignmentService.assignPrimaryRoleByRoleId(saved.getId(), primaryRbacRoleIdOverride);
        } else {
            userRbacAssignmentService.autoAssignPrimaryRoleByUserRole(saved.getId(), saved.getRole());
        }
        return saved;
    }

    private static String normalizeEmail(String email) {
        if (email == null) {
            return "";
        }
        return email.trim().toLowerCase(Locale.ROOT);
    }

    @Transactional
    public User update(UUID id, UpdateUserRequest request) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + id));
        boolean roleChanged = false;
        if (request.getRole() != null) {
            if (!request.getRole().isCompanyDashboardCreatable()) {
                throw new IllegalArgumentException(
                        "Invalid role: only internal company roles are allowed (not CUSTOMER, provider portal roles, or Super Admin).");
            }
            if (request.getRole() != user.getRole()) {
                user.setRole(request.getRole());
                roleChanged = true;
            }
        }
        if (request.getEmail() != null && !request.getEmail().equals(user.getEmail())) {
            if (userRepository.existsByEmail(request.getEmail())) {
                throw new IllegalArgumentException("Email already in use");
            }
            user.setEmail(request.getEmail());
        }
        if (request.getPhone() != null) {
            user.setPhone(request.getPhone());
        }
        if (request.getStatus() != null) {
            user.setStatus(request.getStatus());
        }
        User saved = userRepository.save(user);
        if (roleChanged) {
            // Replaces primary sys_user_roles row with the system role for this UserRole (group_id from sys_roles).
            // Any prior custom RBAC pick for sidebar must be re-applied by an admin if still needed.
            userRbacAssignmentService.autoAssignPrimaryRoleByUserRole(saved.getId(), saved.getRole());
        }
        return saved;
    }

    @Transactional
    public void softDelete(UUID id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + id));
        user.softDelete();
        userRepository.save(user);
    }

    @Transactional
    public void freeze(UUID id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + id));
        user.freeze();
        userRepository.save(user);
    }

    @Transactional
    public void unfreeze(UUID id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + id));
        user.activate();
        userRepository.save(user);
    }

    @Transactional
    public void resetPassword(UUID id, String newPassword) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + id));
        user.setPasswordHash(passwordEncoder.encode(newPassword));
        userRepository.save(user);
    }

    @Transactional
    public void changePassword(UUID id, String currentPassword, String newPassword) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + id));
        if (!passwordEncoder.matches(currentPassword, user.getPasswordHash())) {
            throw new IllegalArgumentException("Current password is incorrect");
        }
        user.setPasswordHash(passwordEncoder.encode(newPassword));
        userRepository.save(user);
    }
}
