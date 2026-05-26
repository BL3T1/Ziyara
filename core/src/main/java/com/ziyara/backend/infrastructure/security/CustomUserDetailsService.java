package com.ziyara.backend.infrastructure.security;

import com.ziyara.backend.domain.entity.User;
import com.ziyara.backend.domain.enums.UserRole;
import com.ziyara.backend.domain.repository.ProviderStaffRepository;
import com.ziyara.backend.domain.repository.UserRepository;
import com.ziyara.backend.domain.repository.UserRoleAssignmentRepository;
import lombok.RequiredArgsConstructor;
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
 * Custom UserDetailsService Implementation
 * Loads user details for Spring Security
 */
@Service
@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;
    private final UserRoleAssignmentRepository userRoleAssignmentRepository;
    private final ProviderStaffRepository providerStaffRepository;

    @Override
    public UserDetails loadUserByUsername(String userId) throws UsernameNotFoundException {
        UUID id = UUID.fromString(userId);
        User user = userRepository.findById(id)
                .orElseThrow(() -> new UsernameNotFoundException("User not found with id: " + userId));

        List<GrantedAuthority> authorities = new ArrayList<>();
        authorities.add(new SimpleGrantedAuthority("ROLE_" + user.getRole().name()));

        for (String code : userRoleAssignmentRepository.findPermissionCodesByUserId(id)) {
            authorities.add(new SimpleGrantedAuthority(code));
        }

        if (isProviderPortalRole(user.getRole())) {
            providerStaffRepository.findByUserId(id).ifPresent(ps -> {
                if (ps.getProviderRole() != null) {
                    authorities.add(new SimpleGrantedAuthority("PROVIDER_ROLE_" + ps.getProviderRole().name()));
                }
            });
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

    private static boolean isProviderPortalRole(UserRole role) {
        return role == UserRole.PROVIDER_MANAGER
                || role == UserRole.PROVIDER_FINANCE
                || role == UserRole.PROVIDER_STAFF
                || role == UserRole.TAXI_OPERATOR;
    }
}
