package com.ziyara.backend.infrastructure.security;

import com.ziyara.backend.domain.entity.User;
import com.ziyara.backend.domain.repository.UserRepository;
import com.ziyara.backend.domain.repository.UserRoleAssignmentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Loads Spring Security authorities for a user.
 *
 * SUPER_ADMIN → ROLE_SUPER_ADMIN + every permission from the SUPER_ADMIN system role.
 * STAFF       → ROLE_STAFF + permissions from the user's assigned role (sys_user_roles → sys_role_permissions).
 * CUSTOMER    → ROLE_CUSTOMER only; no company permissions.
 */
@Service
@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;
    private final UserRoleAssignmentRepository userRoleAssignmentRepository;

    @CacheEvict(value = "userDetails", key = "#userId", cacheManager = "localCacheManager")
    public void evictUserDetails(String userId) {}

    @Override
    @Cacheable(value = "userDetails", key = "#userId", cacheManager = "localCacheManager")
    public UserDetails loadUserByUsername(String userId) throws UsernameNotFoundException {
        UUID id = UUID.fromString(userId);
        User user = userRepository.findById(id)
                .orElseThrow(() -> new UsernameNotFoundException("User not found with id: " + userId));

        List<GrantedAuthority> authorities = new ArrayList<>();
        authorities.add(new SimpleGrantedAuthority("ROLE_" + user.getRole().name()));

        switch (user.getRole()) {
            case SUPER_ADMIN -> {
                for (String code : userRoleAssignmentRepository.findPermissionCodesBySystemRoleCode("SUPER_ADMIN")) {
                    authorities.add(new SimpleGrantedAuthority(code));
                }
            }
            case STAFF -> {
                for (String code : userRoleAssignmentRepository.findPermissionCodesByUserId(id)) {
                    authorities.add(new SimpleGrantedAuthority(code));
                }
            }
            case CUSTOMER -> { /* no company permissions */ }
        }

        return new UserPrincipal(
                user.getId(),
                user.getPasswordHash(),
                user.getTokenVersion(),
                user.getStatus().canLogin(),
                !user.isLocked(),
                authorities
        );
    }
}
