package com.ziyara.backend.infrastructure.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

/**
 * OpenAPI Configuration
 * Configures Swagger/OpenAPI documentation
 */
@Configuration
public class OpenApiConfig {

    /**
     * Must include the servlet context path (e.g. /api/v1). Absolute hosts without it break Swagger UI
     * behind Docker/nginx: the UI would fetch /api-docs on :8080 root (404) instead of /api/v1/api-docs.
     * A same-origin-relative path fixes both direct backend access and dashboard proxy on :3000.
     */
    @Value("${server.servlet.context-path:/api/v1}")
    private String servletContextPath;

    @Bean
    public OpenAPI ziyarahOpenAPI() {
        final String securitySchemeName = "bearerAuth";
        String apiBase = servletContextPath.startsWith("/") ? servletContextPath : "/" + servletContextPath;

        return new OpenAPI()
                .info(apiInfo())
                .servers(List.of(
                        new Server().url(apiBase).description("API base (same host as Swagger / nginx proxy)")
                ))
                .addSecurityItem(new SecurityRequirement().addList(securitySchemeName))
                .components(new Components()
                        .addSecuritySchemes(securitySchemeName, 
                                new SecurityScheme()
                                        .name(securitySchemeName)
                                        .type(SecurityScheme.Type.HTTP)
                                        .scheme("bearer")
                                        .bearerFormat("JWT")
                                        .description("JWT Authorization header using the Bearer scheme")
                        )
                );
    }
    
    private Info apiInfo() {
        return new Info()
                .title("Ziyarah API")
                .description("""
                        ## Ziyarah Digital Booking Ecosystem API
                        
                        A secure, on-premise digital ecosystem for booking services across multiple 
                        hospitality verticals including Hotels, Resorts, Restaurants, Taxis, and Trips.
                        
                        ### Features
                        - Multi-service booking platform
                        - Role-based access control (RBAC)
                        - Multi-currency transaction support
                        - Automated cancellation and refund processing
                        - Real-time messaging
                        
                        ### Authentication
                        All API endpoints require JWT authentication unless otherwise specified.
                        Use the `/auth/login` endpoint to obtain an access token.
                        """)
                .version("1.0.0")
                .contact(new Contact()
                        .name("Ziyarah Development Team")
                        .email("support@ziyarah.com")
                        .url("https://ziyarah.com"))
                .license(new License()
                        .name("Proprietary")
                        .url("https://ziyarah.com/license"));
    }
}
