package com.ziyara.backend.infrastructure.security;

import com.ziyara.backend.application.service.JwtTokenBlocklistService;
import com.ziyara.backend.domain.enums.UserRole;
import com.ziyara.backend.infrastructure.security.JwtCookieProperties;
import com.ziyara.backend.infrastructure.rls.RlsSessionAttributes;
import com.ziyara.backend.infrastructure.rls.RlsSessionContext;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.Ordered;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.UUID;

/**
 * JWT Authentication Filter
 * Intercepts requests and validates JWT tokens (Authorization header or HttpOnly cookie).
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class JwtAuthenticationFilter extends OncePerRequestFilter implements Ordered {

    private static final int ORDER = Ordered.HIGHEST_PRECEDENCE + 10;

    @Override
    public int getOrder() {
        return ORDER;
    }

    private final JwtService jwtService;
    private final UserDetailsService userDetailsService;
    private final SecurityContextRepository securityContextRepository;
    private final JwtCookieProperties jwtCookieProperties;
    private final JwtTokenBlocklistService jwtTokenBlocklistService;

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain
    ) throws ServletException, IOException {

        String jwt = resolveToken(request);
        if (jwt == null || jwt.isBlank()) {
            filterChain.doFilter(request, response);
            return;
        }

        try {
            String userId = jwtService.extractUserId(jwt);
            if (userId != null && jwtService.validateToken(jwt)) {
                String jti = jwtService.extractJti(jwt);
                if (jti != null && jwtTokenBlocklistService.isRevoked(jti)) {
                    log.debug("JWT rejected: revoked jti");
                    filterChain.doFilter(request, response);
                    return;
                }
                applyRlsFromJwt(jwt, userId);
                int tokenVersion = jwtService.extractTokenVersion(jwt);
                UserDetails userDetails = userDetailsService.loadUserByUsername(userId);
                boolean allow = true;
                if (userDetails instanceof UserPrincipal up && up.getTokenVersion() != tokenVersion) {
                    log.warn("JWT rejected: token version mismatch for user {}", userId);
                    allow = false;
                }
                if (allow) {
                    UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(
                            userDetails,
                            null,
                            userDetails.getAuthorities()
                    );
                    authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                    SecurityContextHolder.getContext().setAuthentication(authToken);
                    securityContextRepository.saveContext(SecurityContextHolder.getContext(), request, response);
                }
            } else if (userId != null) {
                log.warn("JWT present but validation failed for userId={}", userId);
            }
            filterChain.doFilter(request, response);
        } catch (Exception e) {
            log.error("Cannot set user authentication: {}", e.getMessage(), e);
            filterChain.doFilter(request, response);
        } finally {
            RlsSessionContext.clear();
        }
    }

    private String resolveToken(HttpServletRequest request) {
        final String authHeader = request.getHeader("Authorization");
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            return authHeader.substring(7).trim();
        }
        if (jwtCookieProperties.isEnabled()) {
            return readCookie(request, jwtCookieProperties.getAccessCookieName());
        }
        return null;
    }

    private static String readCookie(HttpServletRequest request, String name) {
        Cookie[] cookies = request.getCookies();
        if (cookies == null) {
            return null;
        }
        for (Cookie c : cookies) {
            if (name.equals(c.getName())) {
                return c.getValue();
            }
        }
        return null;
    }

    private void applyRlsFromJwt(String jwt, String userIdStr) {
        UUID userId = UUID.fromString(userIdStr);
        String roleName = jwtService.extractRole(jwt);
        UUID providerScope = jwtService.extractProviderScopeId(jwt);
        boolean bypass = true;
        if (roleName != null) {
            try {
                bypass = UserRole.valueOf(roleName).isCompanyDirectoryUser();
            } catch (IllegalArgumentException ex) {
                bypass = true;
            }
        }
        RlsSessionContext.set(new RlsSessionAttributes(bypass, userId, providerScope));
    }
}

