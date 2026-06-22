package com.ziyara.backend.infrastructure.security;

import com.ziyara.backend.domain.enums.UserRole;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Optional;

/**
 * Resolves the coarse {@link UserRole} tier and common permission helpers
 * from the Spring Security context.
 */
public final class SecurityRoleUtils {

    private SecurityRoleUtils() {
    }

    public static Optional<UserRole> currentUserRole() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) {
            return Optional.empty();
        }
        for (GrantedAuthority ga : auth.getAuthorities()) {
            String a = ga.getAuthority();
            if (a == null || !a.startsWith("ROLE_")) {
                continue;
            }
            try {
                return Optional.of(UserRole.valueOf(a.substring("ROLE_".length())));
            } catch (IllegalArgumentException ignored) {
                // not a recognised tier
            }
        }
        return Optional.empty();
    }

    /** True when the current user holds the {@code discounts:approve} permission. */
    public static boolean canActivateOrApproveDiscounts() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) return false;
        return auth.getAuthorities().stream()
                .anyMatch(a -> "discounts:approve".equals(a.getAuthority())
                        || "ROLE_SUPER_ADMIN".equals(a.getAuthority()));
    }

    /** True when the current user holds the {@code providers:approve} permission. */
    public static boolean canApproveProviders() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) return false;
        return auth.getAuthorities().stream()
                .anyMatch(a -> "providers:approve".equals(a.getAuthority())
                        || "ROLE_SUPER_ADMIN".equals(a.getAuthority()));
    }

    /** True when the current user holds the {@code portal:manage} permission. */
    public static boolean hasPortalManage() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) return false;
        return auth.getAuthorities().stream()
                .anyMatch(a -> "portal:manage".equals(a.getAuthority())
                        || "ROLE_SUPER_ADMIN".equals(a.getAuthority()));
    }
}
