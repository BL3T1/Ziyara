package com.ziyara.backend.infrastructure.web;

import com.ziyara.backend.infrastructure.security.JwtCookieProperties;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.ResponseCookie;

/**
 * Sets and clears HttpOnly auth cookies (paths aligned with {@code server.servlet.context-path}).
 */
public final class AuthCookieHelper {

    private AuthCookieHelper() {
    }

    public static void addAuthCookies(HttpServletResponse response,
                                       String accessToken,
                                       String refreshToken,
                                       long accessTtlSeconds,
                                       long refreshTtlSeconds,
                                       String contextPath,
                                       JwtCookieProperties props) {
        String path = normalizePath(contextPath);
        if (accessToken != null && !accessToken.isBlank()) {
            response.addHeader("Set-Cookie", buildCookie(props.getAccessCookieName(), accessToken, accessTtlSeconds, path, props).toString());
        }
        if (refreshToken != null && !refreshToken.isBlank()) {
            response.addHeader("Set-Cookie", buildCookie(props.getRefreshCookieName(), refreshToken, refreshTtlSeconds, path, props).toString());
        }
    }

    public static void clearAuthCookies(HttpServletResponse response, String contextPath, JwtCookieProperties props) {
        String path = normalizePath(contextPath);
        response.addHeader("Set-Cookie", clearCookie(props.getAccessCookieName(), path, props).toString());
        response.addHeader("Set-Cookie", clearCookie(props.getRefreshCookieName(), path, props).toString());
    }

    private static String normalizePath(String contextPath) {
        if (contextPath == null || contextPath.isBlank() || "/".equals(contextPath)) {
            return "/";
        }
        return contextPath.endsWith("/") ? contextPath.substring(0, contextPath.length() - 1) : contextPath;
    }

    private static ResponseCookie buildCookie(String name, String value, long maxAgeSeconds, String path, JwtCookieProperties props) {
        ResponseCookie.ResponseCookieBuilder b = ResponseCookie.from(name, value)
                .httpOnly(true)
                .secure(props.isSecure())
                .path(path)
                .maxAge(maxAgeSeconds)
                .sameSite(props.getSameSite());
        return b.build();
    }

    private static ResponseCookie clearCookie(String name, String path, JwtCookieProperties props) {
        return ResponseCookie.from(name, "")
                .httpOnly(true)
                .secure(props.isSecure())
                .path(path)
                .maxAge(0)
                .sameSite(props.getSameSite())
                .build();
    }
}

