package com.guentours.security;

import com.guentours.user.domain.User;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.List;

/** Adapts the user module's {@link User} aggregate to Spring Security's {@link UserDetails}. */
public class AppUserPrincipal implements UserDetails {

    private final User user;

    public AppUserPrincipal(User user) {
        this.user = user;
    }

    public String getUserId() {
        return user.getId();
    }

    public String getRole() {
        return user.getRole().name();
    }

    /** Null for CUSTOMER/ADMIN accounts; set only for partner accounts (PARTNER_AIRLINE, PARTNER_HOTEL, etc.). */
    public String getPartnerId() {
        return user.getPartnerId();
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority("ROLE_" + user.getRole().name()));
    }

    @Override
    public String getPassword() {
        return user.getPasswordHash();
    }

    @Override
    public String getUsername() {
        return user.getEmail();
    }
}