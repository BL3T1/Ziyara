package com.ziyara.backend.infrastructure.config;

import com.ziyara.backend.application.command.UserCommandHandler;
import com.ziyara.backend.domain.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/**
 * Ensures the mobile test customer account (seeded by V34) has a known password
 * derived from APP_DEMO_PASSWORD so the mobile app can actually log in.
 * Runs after SuperAdminSeeder (Order 100) on every startup.
 */
@Component
@Order(150)
@RequiredArgsConstructor
@Slf4j
public class MobileTestDataSeeder implements ApplicationRunner {

    private static final String MOBILE_TEST_EMAIL = "mobile_test@ziyarah.com";

    private final UserRepository userRepository;
    private final UserCommandHandler userCommandHandler;

    @Override
    public void run(ApplicationArguments args) {
        userRepository.findByEmail(MOBILE_TEST_EMAIL).ifPresent(user -> {
            String password = System.getenv("APP_DEMO_PASSWORD");
            if (password == null || password.isBlank()) {
                log.info("APP_DEMO_PASSWORD not set — mobile test account password unchanged");
                return;
            }
            try {
                userCommandHandler.resetPassword(user.getId(), password);
                log.info("Mobile test account password synced: {}", MOBILE_TEST_EMAIL);
            } catch (Exception e) {
                log.warn("Could not sync mobile test account password: {}", e.getMessage());
            }
        });
    }
}
