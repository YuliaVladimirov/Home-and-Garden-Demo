package org.example.homeandgarden.security.service;

import lombok.RequiredArgsConstructor;
import org.example.homeandgarden.user.repository.UserRepository;

import org.example.homeandgarden.user.entity.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.example.homeandgarden.security.entity.UserDetailsImpl;

@Service
@RequiredArgsConstructor
public class UserDetailsServiceImpl implements UserDetailsService {

    private final UserRepository userRepository;

    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {

        User existingUser = userRepository.findByEmail(email).orElseThrow(() -> new UsernameNotFoundException(String.format("User with email: %s, was not found.", email)));

        if (!existingUser.getIsEnabled()) {
            throw new IllegalArgumentException(String.format("User with email: %s, is unregistered.", email));
        }
        if (!existingUser.getIsNonLocked()) {
            throw new IllegalArgumentException(String.format("User with email: %s, is locked.", email));
        }

        return new UserDetailsImpl(existingUser);
    }
}