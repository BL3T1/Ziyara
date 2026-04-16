package com.ziyara.backend.infrastructure.security;

import com.ziyara.backend.domain.enums.UserRole;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Optional;

/**
 * Resolves {@link UserRole} from Spring Security context (JWT → GrantedAuthority ROLE_*).
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
                // non-enum authority
            }
        }
        return Optional.empty();
    }

    /** Super Admin and CEO may activate discounts without a separate approver. */
    public static boolean canActivateOrApproveDiscounts() {
        return currentUserRole()
                .map(r -> r == UserRole.SUPER_ADMIN || r == UserRole.CEO)
                .orElse(false);
    }
}
