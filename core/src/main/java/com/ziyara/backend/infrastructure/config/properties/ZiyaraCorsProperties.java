package com.ziyara.backend.infrastructure.config.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * CORS allowlist. Set {@code allowed-origins} to a comma-separated list (env {@code ZIYARA_CORS_ALLOWED_ORIGINS}).
 */
@Data
@ConfigurationProperties(prefix = "ziyara.cors")
public class ZiyaraCorsProperties {

    /**
     * Comma-separated allowed origins (e.g. {@code https://app.example.com,https://admin.example.com}).
     */
    private String allowedOrigins = "";

    /**
     * When true and {@link #allowedOrigins} is empty, use localhost dev URLs (5173/3000).
     */
    private boolean allowLocalDefaults = true;

    private List<String> defaultLocalOrigins = List.of(
            "http://localhost:5173",
            "http://127.0.0.1:5173",
            "http://localhost:3000",
            "http://127.0.0.1:3000"
    );

    private boolean allowCredentials = true;

    /**
     * When true, exposes wildcard {@code *} origin (no credentials). For emergency tooling only.
     */
    private boolean allowAllOrigins = false;

    public List<String> resolveAllowedOrigins() {
        if (allowedOrigins != null && !allowedOrigins.isBlank()) {
            return Arrays.stream(allowedOrigins.split(","))
                    .map(String::trim)
                    .filter(s -> !s.isEmpty())
                    .collect(Collectors.toCollection(ArrayList::new));
        }
        if (allowLocalDefaults) {
            return new ArrayList<>(defaultLocalOrigins);
        }
        return new ArrayList<>();
    }
}
