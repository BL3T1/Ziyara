package com.ziyara.backend.application.command;

import com.ziyara.backend.application.annotation.Audited;
import com.ziyara.backend.application.dto.request.CreateUserRequest;
import com.ziyara.backend.application.dto.request.UpdateUserRequest;
import com.ziyara.backend.application.service.CompanyStaffRoleCatalogService;
import com.ziyara.backend.application.service.PasswordHistoryService;
import com.ziyara.backend.application.service.PasswordPolicyService;
import com.ziyara.backend.application.service.UserPasswordService;
import com.ziyara.backend.application.service.UserRbacAssignmentService;
import com.ziyara.backend.domain.enums.NotificationType;
import com.ziyara.backend.modules.notification.api.StaffNotificationCommandPublisher;
import com.ziyara.backend.application.dto.StaffNotificationEvent;
import com.ziyara.backend.domain.entity.Role;
import com.ziyara.backend.domain.entity.User;
import com.ziyara.backend.domain.enums.UserRole;
import com.ziyara.backend.domain.enums.UserStatus;
import com.ziyara.backend.domain.repository.RoleRepository;
import com.ziyara.backend.domain.repository.UserRepository;
import com.ziyara.backend.domain.repository.UserRoleAssignmentRepository;
import lombok.RequiredArgsConstructor;
import org.jooq.DSLContext;
import org.jooq.impl.DSL;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
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
    private final StaffNotificationCommandPublisher staffNotificationCommandPublisher;
    private final PasswordHistoryService passwordHistoryService;
    private final PasswordPolicyService passwordPolicyService;
    private final UserPasswordService userPasswordService;
    private final UserRoleAssignmentRepository userRoleAssignmentRepository;
    private final DSLContext dsl;

    /**
     * Company dashboard / HR: internal staff via legacy {@code role} enum or unified {@code primaryRbacRoleId}.
     */
    /**
     * Company dashboard / HR: creates a STAFF user and assigns the given role.
     * {@code primaryRbacRoleId} is required — must reference an active role in sys_roles.
     */
    @Audited(action = "USER_CREATE", entityType = "User")
    @Transactional
    public UUID create(CreateUserRequest request) {
        if (request.getPrimaryRbacRoleId() == null) {
            throw new IllegalArgumentException("primaryRbacRoleId is required.");
        }
        Role rbac = roleRepository.findById(request.getPrimaryRbacRoleId())
                .orElseThrow(() -> new IllegalArgumentException("Role not found"));
        if (!companyStaffRoleCatalogService.isEligibleForCompanyUserCreationPrimaryRole(rbac)) {
            throw new IllegalArgumentException(
                    "This role cannot be assigned (inactive or reserved for system use).");
        }
        User saved = createInternal(request, UserRole.STAFF, rbac.getId());
        staffNotificationCommandPublisher.publishAfterCommit(StaffNotificationEvent.builder()
                .eventId(UUID.randomUUID())
                .notificationType(NotificationType.STAFF_USER_CREATED.name())
                .title("New staff user")
                .message("A company staff account was created: " + saved.getEmail())
                .notifyRoles(List.of("SUPER_ADMIN"))
                .metadata("{\"userId\":\"" + saved.getId() + "\"}")
                .build());
        return saved.getId();
    }

    /**
     * Bootstrap and demo seeding: allows any {@link UserRole} (CUSTOMER, SUPER_ADMIN, STAFF).
     */
    @Transactional
    public User createForBootstrap(CreateUserRequest request, UserRole role) {
        if (role == null) {
            throw new IllegalArgumentException("Bootstrap user creation requires role");
        }
        return createInternalWithForce(request, role, null, false);
    }

    private User createInternal(CreateUserRequest request, UserRole resolvedRole, UUID primaryRbacRoleIdOverride) {
        return createInternalWithForce(request, resolvedRole, primaryRbacRoleIdOverride, true);
    }

    private User createInternalWithForce(CreateUserRequest request, UserRole resolvedRole, UUID primaryRbacRoleIdOverride, boolean requirePasswordChange) {
        String emailNorm = normalizeEmail(request.getEmail());
        if (userRepository.existsByEmail(emailNorm)) {
            throw new IllegalArgumentException("User with email already exists: " + emailNorm);
        }
        String usernameNorm = request.getUsername() != null ? request.getUsername().trim() : null;
        if (usernameNorm != null && !usernameNorm.isBlank() && userRepository.existsByUsername(usernameNorm)) {
            throw new IllegalArgumentException("Username already taken: " + usernameNorm);
        }
        if (request.getPhone() != null && !request.getPhone().isBlank() && userRepository.existsByPhone(request.getPhone())) {
            throw new IllegalArgumentException("User with phone already exists");
        }

        passwordPolicyService.assertAcceptable(request.getPassword());

        User user = new User();
        // Do not set id - let JPA generate it so persist() is used for new users
        user.setEmail(emailNorm);
        if (usernameNorm != null && !usernameNorm.isBlank()) {
            user.setUsername(usernameNorm);
        }
        if (request.getFirstName() != null && !request.getFirstName().isBlank()) {
            user.setFirstName(request.getFirstName().trim());
        }
        if (request.getLastName() != null && !request.getLastName().isBlank()) {
            user.setLastName(request.getLastName().trim());
        }
        user.setPhone(request.getPhone());
        user.setPasswordHash(passwordEncoder.encode(request.getPassword()));
        user.setRole(resolvedRole);
        // Company-created staff: default ACTIVE when status omitted so they can sign in immediately
        String statusRaw = request.getStatus();
        boolean active = statusRaw == null
                || statusRaw.isBlank()
                || "ACTIVE".equalsIgnoreCase(statusRaw.trim());
        user.setStatus(active ? UserStatus.ACTIVE : UserStatus.PENDING_VERIFICATION);
        user.setMustChangePassword(requirePasswordChange);
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

    @CacheEvict(value = "userProfile", key = "#id")
    @Audited(action = "USER_UPDATE", entityType = "User", entityIdArgIndex = 0)
    @Transactional
    public User update(UUID id, UpdateUserRequest request) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + id));
        if (request.getEmail() != null && !request.getEmail().equals(user.getEmail())) {
            if (userRepository.existsByEmail(request.getEmail())) {
                throw new IllegalArgumentException("Email already in use");
            }
            user.setEmail(request.getEmail());
        }
        if (request.getUsername() != null) {
            String newUsername = request.getUsername().trim();
            if (!newUsername.equals(user.getUsername() != null ? user.getUsername() : "")) {
                if (!newUsername.isBlank() && userRepository.existsByUsername(newUsername)) {
                    throw new IllegalArgumentException("Username already taken: " + newUsername);
                }
                user.setUsername(newUsername.isBlank() ? null : newUsername);
            }
        }
        if (request.getPhone() != null) {
            user.setPhone(request.getPhone());
        }
        if (request.getStatus() != null) {
            user.setStatus(request.getStatus());
        }
        // Always persist first/last name to sys_users (for staff) in addition to customers (for B2C).
        boolean hasFirstName = request.getFirstName() != null && !request.getFirstName().isBlank();
        boolean hasLastName  = request.getLastName()  != null && !request.getLastName().isBlank();
        if (hasFirstName) user.setFirstName(request.getFirstName().trim());
        if (hasLastName)  user.setLastName(request.getLastName().trim());
        User saved = userRepository.save(user);
        if (hasFirstName || hasLastName) {
            boolean customerRowExists = dsl.fetchExists(
                    dsl.selectOne().from(DSL.table(DSL.name("customers")))
                            .where(DSL.field(DSL.name("customers", "user_id"), UUID.class).eq(id)));
            if (customerRowExists) {
                var upd = dsl.update(DSL.table(DSL.name("customers")));
                if (hasFirstName && hasLastName) {
                    upd.set(DSL.field(DSL.name("first_name"), String.class), request.getFirstName().trim())
                       .set(DSL.field(DSL.name("last_name"),  String.class), request.getLastName().trim())
                       .where(DSL.field(DSL.name("customers", "user_id"), UUID.class).eq(id))
                       .execute();
                } else if (hasFirstName) {
                    upd.set(DSL.field(DSL.name("first_name"), String.class), request.getFirstName().trim())
                       .where(DSL.field(DSL.name("customers", "user_id"), UUID.class).eq(id))
                       .execute();
                } else {
                    upd.set(DSL.field(DSL.name("last_name"), String.class), request.getLastName().trim())
                       .where(DSL.field(DSL.name("customers", "user_id"), UUID.class).eq(id))
                       .execute();
                }
            }
        }
        return saved;
    }

    @CacheEvict(value = "userProfile", key = "#id")
    @Audited(action = "USER_DELETE", entityType = "User", entityIdArgIndex = 0)
    @Transactional
    public void softDelete(UUID id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + id));
        user.softDelete();
        userRepository.save(user);
        userRoleAssignmentRepository.clearAssignmentsForUser(id);
    }

    @Audited(action = "USER_FREEZE", entityType = "User", entityIdArgIndex = 0)
    @Transactional
    public void freeze(UUID id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + id));
        user.freeze();
        userRepository.save(user);
        staffNotificationCommandPublisher.publishAfterCommit(StaffNotificationEvent.builder()
                .eventId(UUID.randomUUID())
                .notificationType(NotificationType.SYSTEM_ALERT.name())
                .title("Account blocked")
                .message("Your company dashboard account has been blocked. Contact HR or Super Admin if this is unexpected.")
                .recipientUserId(id)
                .build());
    }

    @Audited(action = "USER_UNFREEZE", entityType = "User", entityIdArgIndex = 0)
    @Transactional
    public void unfreeze(UUID id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + id));
        user.activate();
        userRepository.save(user);
    }

    @Audited(action = "USER_PASSWORD_RESET", entityType = "User", entityIdArgIndex = 0)
    @Transactional
    public void resetPassword(UUID id, String newPassword) {
        userPasswordService.resetPassword(id, newPassword);
    }

    @Transactional
    public void updateFcmToken(UUID id, String fcmToken) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + id));
        user.setFcmToken(fcmToken);
        userRepository.save(user);
    }

    @Audited(action = "USER_PASSWORD_CHANGE", entityType = "User", entityIdArgIndex = 0)
    @Transactional
    public void changePassword(UUID id, String currentPassword, String newPassword) {
        passwordPolicyService.assertAcceptable(newPassword);
        User user = userRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + id));
        if (!user.isMustChangePassword()) {
            // Regular change: current password is mandatory
            if (currentPassword == null || currentPassword.isBlank()) {
                throw new IllegalArgumentException("Current password is required");
            }
            if (!passwordEncoder.matches(currentPassword, user.getPasswordHash())) {
                throw new IllegalArgumentException("Current password is incorrect");
            }
        }
        passwordHistoryService.assertPasswordNotReused(id, newPassword, passwordEncoder, user.getPasswordHash());
        passwordHistoryService.recordPasswordRotation(id, user.getPasswordHash());
        user.setPasswordHash(passwordEncoder.encode(newPassword));
        user.setLastPasswordChange(LocalDateTime.now());
        user.setMustChangePassword(false);
        user.incrementTokenVersion();
        userRepository.save(user);
    }

    @Transactional
    public void ensureUsername(UUID id, String username) {
        userRepository.findById(id).ifPresent(user -> {
            if (user.getUsername() == null || user.getUsername().isBlank()) {
                user.setUsername(username.trim());
                userRepository.save(user);
            }
        });
    }

    @Transactional
    public void clearMustChangePasswordFlag(UUID id) {
        userRepository.findById(id).ifPresent(user -> {
            if (user.isMustChangePassword()) {
                user.setMustChangePassword(false);
                userRepository.save(user);
            }
        });
    }

    @Transactional
    public void ensureFirstLastName(UUID id, String firstName, String lastName) {
        userRepository.findById(id).ifPresent(user -> {
            boolean changed = false;
            if (firstName != null && !firstName.isBlank() &&
                    (user.getFirstName() == null || user.getFirstName().isBlank())) {
                user.setFirstName(firstName.trim());
                changed = true;
            }
            if (lastName != null && !lastName.isBlank() &&
                    (user.getLastName() == null || user.getLastName().isBlank())) {
                user.setLastName(lastName.trim());
                changed = true;
            }
            if (changed) userRepository.save(user);
        });
    }
}
