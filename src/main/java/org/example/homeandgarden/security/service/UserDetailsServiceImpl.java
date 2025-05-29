package org.example.homeandgarden.security.service;

import lombok.RequiredArgsConstructor;
import org.example.homeandgarden.user.repository.UserRepository;

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

        return userRepository.findByEmail(email)
                .map(UserDetailsImpl::new)
                .orElseThrow(()->new UsernameNotFoundException(String.format("User with email: %s, was not found.", email)));
    }
}