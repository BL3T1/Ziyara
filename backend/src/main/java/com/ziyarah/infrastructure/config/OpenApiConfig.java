package com.ziyarah.infrastructure.config;

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
    
    @Value("${server.port:8080}")
    private String serverPort;
    
    @Bean
    public OpenAPI ziyarahOpenAPI() {
        final String securitySchemeName = "bearerAuth";
        
        return new OpenAPI()
                .info(apiInfo())
                .servers(List.of(
                        new Server().url("http://localhost:" + serverPort).description("Local server"),
                        new Server().url("http://localhost:8080").description("Docker server")
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
