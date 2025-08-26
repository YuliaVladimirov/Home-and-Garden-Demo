package org.example.homeandgarden.security.entity;

import lombok.*;

import org.example.homeandgarden.user.entity.User;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.List;

@Getter
public class UserDetailsImpl implements UserDetails {

    private final String email;
    private final String passwordHash;
    private final String role;
    private final boolean isEnabled;
    private final boolean isNonLocked;

    public UserDetailsImpl(User user) {
        this.email = user.getEmail();
        this.passwordHash = user.getPasswordHash();
        this.role = "ROLE_" + user.getUserRole().name();
        this.isEnabled = user.getIsEnabled();
        this.isNonLocked = user.getIsNonLocked();
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority(role));
    }

    @Override
    public String getPassword() {
        return passwordHash;
    }

    @Override
    public String getUsername() {
        return email;
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return isNonLocked;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return isEnabled;
    }
}

