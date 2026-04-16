package com.ziyara.backend;

import com.ziyara.backend.infrastructure.config.MediaStorageProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

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
@EnableConfigurationProperties(MediaStorageProperties.class)
public class ZiyarahApplication {
    
    public static void main(String[] args) {
        SpringApplication.run(ZiyarahApplication.class, args);
    }
}
