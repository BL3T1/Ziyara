package com.ziyara.backend.infrastructure.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.web.context.RequestAttributeSecurityContextRepository;
import org.springframework.security.web.context.SecurityContextRepository;

/**
 * Separate from {@link SecurityConfig} so {@link com.ziyara.backend.infrastructure.security.JwtAuthenticationFilter}
 * can depend on this bean without a cycle (SecurityConfig also references the JWT filter).
 */
@Configuration
public class SecurityContextRepositoryConfig {

	@Bean
	public SecurityContextRepository securityContextRepository() {
		return new RequestAttributeSecurityContextRepository();
	}
}
