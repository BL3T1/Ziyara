package com.ziyarah;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Ziyarah Backend Application
 * Main entry point for the Spring Boot application
 * 
 * Clean Architecture Structure:
 * - Domain Layer: com.ziyarah.domain.* (entities, use cases, repository interfaces)
 * - Application Layer: com.ziyarah.application.* (DTOs, services)
 * - Infrastructure Layer: com.ziyarah.infrastructure.* (JPA, config, security)
 * - Presentation Layer: com.ziyarah.presentation.* (controllers, exception handling)
 */
@SpringBootApplication
public class ZiyarahApplication {
    
    public static void main(String[] args) {
        SpringApplication.run(ZiyarahApplication.class, args);
    }
}
