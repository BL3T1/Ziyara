package com.ziyara.backend.infrastructure.config;

import com.ziyara.backend.application.command.UserCommandHandler;
import com.ziyara.backend.application.dto.request.CreateUserRequest;
import com.ziyara.backend.application.service.UserRbacAssignmentService;
import com.ziyara.backend.domain.enums.UserRole;
import com.ziyara.backend.domain.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.security.SecureRandom;
import java.util.Base64;
import java.util.List;
import java.util.UUID;

/**
 * Ensures the super admin demo account exists and has the known password.
 * Runs on every startup regardless of profile, unless disabled via config.
 * Set app.demo.super-admin.enabled=false (e.g. in prod) to skip.
 */
@Component
@Order(100)
@RequiredArgsConstructor
@Slf4j
@ConditionalOnProperty(name = "app.demo.super-admin.enabled", havingValue = "true", matchIfMissing = true)
public class SuperAdminSeeder implements ApplicationRunner {

    private static final String BOOTSTRAP_PASSWORD_ENV = "APP_DEMO_PASSWORD";
    private static final SecureRandom RNG = new SecureRandom();

    private final UserCommandHandler userCommandHandler;
    private final UserRepository userRepository;
    private final UserRbacAssignmentService userRbacAssignmentService;

    private record AdminAccount(String email, String username, String firstName, String lastName) {}

    private static final List<AdminAccount> ADMIN_ACCOUNTS = List.of(
            new AdminAccount(DemoDataSeeder.SUPER_ADMIN_EMAIL, "Administrator", "System",    "Administrator"),
            new AdminAccount("admin@ziyarah.com",              "Admin",         "Admin",     null),
            new AdminAccount("developer@ziyarah.com",          "Developer",     null,        null)
    );

    @Override
    public void run(ApplicationArguments args) {
        String bootstrapPassword = resolveBootstrapPassword();
        for (AdminAccount account : ADMIN_ACCOUNTS) {
            ensureAdminAccount(account, bootstrapPassword);
        }
    }

    private void ensureAdminAccount(AdminAccount account, String password) {
        var existing = userRepository.findByEmail(account.email());
        if (existing.isPresent()) {
            UUID id = existing.get().getId();
            // Reset password in its own try — may throw if the same password is already set (reuse check).
            try {
                userCommandHandler.resetPassword(id, password);
            } catch (Exception e) {
                log.warn("Could not reset password for {}: {}", account.email(), e.getMessage());
            }
            // Always ensure metadata, clear force-change flag, and (re-)wire the role assignment.
            // The role-assignment call also evicts the Redis userPermissions cache so a stale
            // empty-permissions entry from before V53 does not survive the restart.
            try {
                userCommandHandler.ensureUsername(id, account.username());
                userCommandHandler.ensureFirstLastName(id, account.firstName(), account.lastName());
                userCommandHandler.clearMustChangePasswordFlag(id);
                userRbacAssignmentService.autoAssignPrimaryRoleByUserRole(id, UserRole.SUPER_ADMIN);
                log.info("Admin account ensured: {}", account.email());
            } catch (Exception e) {
                log.warn("Could not ensure admin account {}: {}", account.email(), e.getMessage());
            }
            return;
        }
        CreateUserRequest request = new CreateUserRequest();
        request.setEmail(account.email());
        request.setUsername(account.username());
        request.setFirstName(account.firstName());
        request.setLastName(account.lastName());
        request.setPassword(password);
        request.setStatus("ACTIVE");
        try {
            userCommandHandler.createForBootstrap(request, UserRole.SUPER_ADMIN);
            log.info("Admin account created: {} ({} {})", account.email(), account.firstName(), account.lastName());
        } catch (Exception e) {
            log.warn("Could not create admin account {}: {}", account.email(), e.getMessage());
        }
    }

    private static String resolveBootstrapPassword() {
        String fromEnv = System.getenv(BOOTSTRAP_PASSWORD_ENV);
        if (fromEnv != null && !fromEnv.isBlank()) {
            return fromEnv;
        }

        if (isProdProfileActive()) {
            throw new IllegalStateException(
                    BOOTSTRAP_PASSWORD_ENV + " must be set in prod when super-admin seeding is enabled");
        }

        String generated = generateBootstrapPassword();
        // Only non-prod profiles reach here. This is intended as a local/test bootstrap.
        log.warn("Generated APP bootstrap password for local/test usage (set {} to keep it stable): {}",
                BOOTSTRAP_PASSWORD_ENV, generated);
        return generated;
    }

    private static String generateBootstrapPassword() {
        byte[] buf = new byte[24];
        RNG.nextBytes(buf);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(buf);
    }

    private static boolean isProdProfileActive() {
        String active = System.getProperty("spring.profiles.active");
        if (active == null || active.isBlank()) {
            active = System.getenv("SPRING_PROFILES_ACTIVE");
        }
        if (active == null) return false;
        return java.util.Arrays.stream(active.split(","))
                .map(String::trim)
                .anyMatch(p -> p.equalsIgnoreCase("prod"));
    }
}
