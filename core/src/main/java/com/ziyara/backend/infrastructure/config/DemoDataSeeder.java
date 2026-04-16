package com.ziyara.backend.infrastructure.config;

import com.ziyara.backend.application.command.UserCommandHandler;
import com.ziyara.backend.application.dto.request.CreateUserRequest;
import com.ziyara.backend.domain.enums.UserRole;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.security.SecureRandom;
import java.util.Base64;

/**
 * Seeds one demo user per role for local/testing. Only runs when profile "prod" is NOT active.
 * Use -Dspring.profiles.active=prod to skip in production.
 * Super admin is ensured by {@link SuperAdminSeeder} (runs regardless of profile).
 */
@Component
@Order(200)
@RequiredArgsConstructor
@Slf4j
@Profile("!prod & !functest")
public class DemoDataSeeder implements ApplicationRunner {

    private static final String BOOTSTRAP_PASSWORD_ENV = "APP_DEMO_PASSWORD";
    private static final String EMAIL_DOMAIN = "ziyarah.com";
    public static final String SUPER_ADMIN_EMAIL = "super_admin@" + EMAIL_DOMAIN;
    private static final SecureRandom RNG = new SecureRandom();

    private final UserCommandHandler userCommandHandler;

    @Override
    public void run(ApplicationArguments args) {
        String bootstrapPassword = resolveBootstrapPassword();
        for (UserRole role : UserRole.values()) {
            // SuperAdminSeeder (Order 100) already creates super_admin@ziyarah.com
            if (role == UserRole.SUPER_ADMIN) continue;
            String email = role.name().toLowerCase() + "@" + EMAIL_DOMAIN;
            CreateUserRequest request = new CreateUserRequest();
            request.setEmail(email);
            request.setPassword(bootstrapPassword);
            request.setRole(role);
            request.setStatus("ACTIVE");
            try {
                userCommandHandler.createForBootstrap(request);
                log.info("Demo user created: {} (role: {})", email, role);
            } catch (IllegalArgumentException e) {
                if (e.getMessage() != null && e.getMessage().contains("already exists")) {
                    log.debug("Demo user already exists: {}", email);
                } else {
                    log.warn("Could not create demo user {}: {}", email, e.getMessage());
                }
            }
        }
    }

    private static String resolveBootstrapPassword() {
        String fromEnv = System.getenv(BOOTSTRAP_PASSWORD_ENV);
        if (fromEnv != null && !fromEnv.isBlank()) {
            return fromEnv;
        }

        String generated = generateBootstrapPassword();
        log.warn("Generated demo bootstrap password for local usage (set {} to keep it stable): {}", BOOTSTRAP_PASSWORD_ENV, generated);
        return generated;
    }

    private static String generateBootstrapPassword() {
        byte[] buf = new byte[24];
        RNG.nextBytes(buf);
        // URL-safe string suitable for password fields.
        return Base64.getUrlEncoder().withoutPadding().encodeToString(buf);
    }
}
