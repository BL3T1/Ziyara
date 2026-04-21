package com.ziyara.backend.infrastructure.config;

import com.ziyara.backend.application.service.JwtTokenBlocklistService;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;

/**
 * Test-scoped beans required by security/JWT wiring in {@code @WebMvcTest}.
 */
@TestConfiguration
public class WebMvcSecuritySliceConfiguration {

    @Bean
    public JwtTokenBlocklistService jwtTokenBlocklistService() {
        return new JwtTokenBlocklistService(null);
    }
}
