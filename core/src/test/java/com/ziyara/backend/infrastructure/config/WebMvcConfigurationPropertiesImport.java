package com.ziyara.backend.infrastructure.config;

import com.ziyara.backend.infrastructure.config.properties.JwtCookieProperties;
import com.ziyara.backend.infrastructure.config.properties.ZiyaraCorsProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.TestConfiguration;

@TestConfiguration
@EnableConfigurationProperties({ZiyaraCorsProperties.class, JwtCookieProperties.class})
public class WebMvcConfigurationPropertiesImport {
}
