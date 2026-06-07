package com.ziyara.backend.infrastructure.web;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * Baseline security response headers.
 *
 * Header rationale:
 *   X-Frame-Options DENY          — prevents clickjacking via iframes
 *   X-Content-Type-Options        — stops MIME-type sniffing (mitigates drive-by XSS)
 *   X-XSS-Protection              — legacy browser XSS filter (belt-and-suspenders)
 *   Referrer-Policy               — leaks only origin on cross-origin requests
 *   Permissions-Policy            — denies access to sensitive browser APIs
 *   Content-Security-Policy       — restricts resource origins; prevents injected scripts
 *   Strict-Transport-Security     — HTTPS upgrade + caching (enabled in production only)
 *   X-Content-Type-Options        — never infer content type from response body
 *
 * HTTPS redirect: when httpsEnforce=true (production), plain-HTTP requests are
 * redirected 301 to their HTTPS equivalent before any business logic runs.
 */
public class SecurityHeadersFilter extends OncePerRequestFilter {

    private static final String CSP =
            "default-src 'self'; " +
            "script-src 'self' 'unsafe-inline'; " +   // inline scripts needed for Vite-injected chunks
            "style-src 'self' 'unsafe-inline'; " +
            "img-src 'self' data: blob: https:; " +
            "font-src 'self' data:; " +
            "connect-src 'self'; " +
            "frame-ancestors 'none'; " +               // equivalent to X-Frame-Options DENY
            "base-uri 'self'; " +
            "form-action 'self'";

    private final boolean hstsEnabled;
    // When true, redirect plain HTTP to HTTPS (production only — never enable behind a TLS terminator that already enforces HTTPS)
    private final boolean httpsEnforce;

    public SecurityHeadersFilter(boolean hstsEnabled, boolean httpsEnforce) {
        this.hstsEnabled = hstsEnabled;
        this.httpsEnforce = httpsEnforce;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        // Redirect HTTP → HTTPS before anything else touches the response
        if (httpsEnforce && !isSecure(request)) {
            String httpsUrl = "https://" + request.getServerName() + request.getRequestURI();
            String query = request.getQueryString();
            if (query != null) httpsUrl += "?" + query;
            response.setStatus(HttpServletResponse.SC_MOVED_PERMANENTLY);
            response.setHeader("Location", httpsUrl);
            return;
        }

        // Block rendering inside iframes — mitigates clickjacking
        response.setHeader("X-Frame-Options", "DENY");

        // Prevent MIME-type sniffing — mitigates content-confusion XSS
        response.setHeader("X-Content-Type-Options", "nosniff");

        // Legacy browser XSS filter (belt-and-suspenders; modern browsers ignore this)
        response.setHeader("X-XSS-Protection", "1; mode=block");

        // Limit referrer information leaked to third-party sites
        response.setHeader("Referrer-Policy", "strict-origin-when-cross-origin");

        // Deny access to browser APIs that are not needed by this API
        response.setHeader("Permissions-Policy", "camera=(), microphone=(), geolocation=(), payment=()");

        // Restrict where scripts/styles/images can be loaded from
        response.setHeader("Content-Security-Policy", CSP);

        // HSTS: tell browsers to only connect over HTTPS for the next 365 days
        // Only set when the connection IS already HTTPS to avoid poisoning HTTP clients
        if (hstsEnabled && isSecure(request)) {
            response.setHeader("Strict-Transport-Security", "max-age=31536000; includeSubDomains; preload");
        }

        filterChain.doFilter(request, response);
    }

    private static boolean isSecure(HttpServletRequest request) {
        if (request.isSecure()) return true;
        // Honour X-Forwarded-Proto set by a trusted reverse proxy (nginx / Traefik)
        String proto = request.getHeader("X-Forwarded-Proto");
        return "https".equalsIgnoreCase(proto);
    }
}
