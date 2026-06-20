package com.ziyara.backend;

import com.ziyara.backend.infrastructure.config.MediaStorageProperties;
import com.ziyara.backend.infrastructure.security.JwtCookieProperties;
import com.ziyara.backend.infrastructure.config.properties.ZiyaraCorsProperties;
import com.ziyara.backend.infrastructure.config.properties.ZiyaraPasswordPolicyProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Ziyarah Backend Application
 * Main entry point for the Spring Boot application
 * 
 * Clean Architecture Structure:
 * - Domain Layer: com.ziyara.backend.domain.* (entities, use cases, repository interfaces)
 * - Application Layer: com.ziyara.backend.application.* (DTOs, services)
 * - Infrastructure Layer: com.ziyara.backend.infrastructure.* (JPA, config, security)
 * - Presentation Layer: com.ziyara.backend.presentation.* (controllers, exception handling)
 */
@SpringBootApplication
@EnableConfigurationProperties({
        MediaStorageProperties.class,
        ZiyaraCorsProperties.class,
        JwtCookieProperties.class,
        ZiyaraPasswordPolicyProperties.class
})
@EnableScheduling
@EnableAsync
public class ZiyarahApplication {
    
    public static void main(String[] args) {
        SpringApplication.run(ZiyarahApplication.class, args);
    }
}

