package com.ziyara.backend.infrastructure.config;

import com.ziyara.backend.application.command.UserCommandHandler;
import com.ziyara.backend.application.dto.request.CreateUserRequest;
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

    @Override
    public void run(ApplicationArguments args) {
        String bootstrapPassword = resolveBootstrapPassword();
        String email = DemoDataSeeder.SUPER_ADMIN_EMAIL;
        var existing = userRepository.findByEmail(email);
        if (existing.isPresent()) {
            try {
                userCommandHandler.resetPassword(existing.get().getId(), bootstrapPassword);
                log.info("Super admin demo account password set for {}", email);
            } catch (Exception e) {
                log.warn("Could not reset super admin password: {}", e.getMessage());
            }
            return;
        }
        CreateUserRequest request = new CreateUserRequest();
        request.setEmail(email);
        request.setPassword(bootstrapPassword);
        request.setRole(UserRole.SUPER_ADMIN);
        request.setStatus("ACTIVE");
        try {
            userCommandHandler.createForBootstrap(request);
            log.info("Super admin demo account created: {}", email);
        } catch (Exception e) {
            log.warn("Could not create super admin demo user: {}", e.getMessage());
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
