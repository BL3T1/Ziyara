package com.ziyara.backend.infrastructure.security;

import lombok.Getter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.UUID;

/**
 * UserDetails carrying {@code tokenVersion} for JWT {@code tv} claim validation.
 */
@Getter
public class UserPrincipal implements UserDetails {

    private final UUID id;
    private final String passwordHash;
    private final int tokenVersion;
    private final boolean active;
    private final boolean accountNonLocked;
    private final Collection<? extends GrantedAuthority> authorities;

    public UserPrincipal(
            UUID id,
            String passwordHash,
            int tokenVersion,
            boolean active,
            boolean accountNonLocked,
            Collection<? extends GrantedAuthority> authorities) {
        this.id = id;
        this.passwordHash = passwordHash;
        this.tokenVersion = tokenVersion;
        this.active = active;
        this.accountNonLocked = accountNonLocked;
        this.authorities = authorities;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return authorities;
    }

    @Override
    public String getPassword() {
        return passwordHash;
    }

    @Override
    public String getUsername() {
        return id.toString();
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return accountNonLocked;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return active;
    }
}
