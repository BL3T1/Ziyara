package com.ziyara.backend.infrastructure.config;

import com.ziyara.backend.application.service.JwtTokenBlocklistService;
import com.ziyara.backend.infrastructure.security.JwtIdleTimeoutService;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.security.web.context.DelegatingSecurityContextRepository;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.context.RequestAttributeSecurityContextRepository;
import org.springframework.security.web.context.SecurityContextRepository;

/**
 * Test-scoped beans required by security/JWT wiring in {@code @WebMvcTest}.
 */
@TestConfiguration
public class WebMvcSecuritySliceConfiguration {

    @Bean
    public JwtTokenBlocklistService jwtTokenBlocklistService() {
        return new JwtTokenBlocklistService(null);
    }

    @Bean
    public JwtIdleTimeoutService jwtIdleTimeoutService() {
        return new JwtIdleTimeoutService(30);
    }

    /**
     * Spring Security 6 default repository: delegates to request-attributes first
     * (used by {@code @WithMockUser} in MockMvc tests), then to the HTTP session.
     * Individual {@code SecurityBeans} inner classes must NOT re-declare this bean.
     */
    @Bean
    public SecurityContextRepository securityContextRepository() {
        return new DelegatingSecurityContextRepository(
                new RequestAttributeSecurityContextRepository(),
                new HttpSessionSecurityContextRepository()
        );
    }
}
