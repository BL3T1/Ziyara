package com.ziyara.backend.infrastructure.config.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * HttpOnly JWT cookies; browser sends them on {@code withCredentials} requests.
 */
@Data
@ConfigurationProperties(prefix = "jwt.cookie")
public class JwtCookieProperties {

    private boolean enabled = true;

    private String accessCookieName = "ziyara_access";

    private String refreshCookieName = "ziyara_refresh";

    /**
     * Lax is appropriate for same-site SPA + API on same parent domain; Strict if fully same-origin.
     */
    private String sameSite = "Lax";

    /**
     * Secure flag; enable in production (HTTPS).
     */
    private boolean secure = false;

    /**
     * When false, access/refresh token strings are omitted from JSON (cookie-only clients).
     */
    private boolean alsoReturnTokenInBody = true;
}
