package com.ziyara.backend.infrastructure.security;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * Helper for method security: check if current user is acting on their own resource.
 */
@Component("userSecurity")
public class UserSecurity {

    public boolean isSelf(UUID userId) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || "anonymousUser".equals(auth.getPrincipal())) {
            return false;
        }
        String name = auth.getName();
        if (name == null) return false;
        try {
            return UUID.fromString(name).equals(userId);
        } catch (IllegalArgumentException e) {
            return false;
        }
    }
}
