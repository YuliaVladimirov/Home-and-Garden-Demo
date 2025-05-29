package org.example.homeandgarden.security.service;

import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.homeandgarden.exception.DataNotFoundException;
import org.example.homeandgarden.security.config.JwtService;
import org.example.homeandgarden.security.dto.LoginRequest;
import org.example.homeandgarden.security.dto.LoginResponse;
import org.example.homeandgarden.security.dto.RefreshRequest;
import org.example.homeandgarden.security.dto.RefreshResponse;
import org.example.homeandgarden.user.entity.User;
import org.example.homeandgarden.user.repository.UserRepository;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthServiceImpl implements AuthService {

        private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;
        private final UserRepository userRepository;

        @Override
        @Transactional
        public LoginResponse login(LoginRequest loginRequest) {

                Authentication authentication;
                try {
                        authentication = authenticationManager
                                .authenticate(new UsernamePasswordAuthenticationToken(loginRequest.getEmail(), loginRequest.getPassword()));
                } catch (AuthenticationException exception) {
                        log.error(String.format("⚠️ Authentication failed for email: %s — reason: bad credentials", loginRequest.getEmail()));
                        throw new BadCredentialsException("Invalid email or password.");
                }

                SecurityContextHolder.getContext().setAuthentication(authentication);

                UserDetails userDetails = (UserDetails) authentication.getPrincipal();
                final String accessToken = jwtService.generateAccessToken(userDetails.getUsername());
                final String refreshToken = jwtService.generateRefreshToken(userDetails.getUsername());

                User logedInUser = userRepository.findByEmail(userDetails.getUsername()).orElseThrow(() -> new DataNotFoundException(String.format("User with email: %s, was not found.", userDetails.getUsername())));

                logedInUser.setRefreshToken(refreshToken);
                User savedUser = userRepository.saveAndFlush(logedInUser);

                if (!savedUser.getRefreshToken().equals(refreshToken)) {
                        log.error(String.format("⚠️ Refresh token was generated but not saved for user with email: %s", loginRequest.getEmail()));
                        throw new IllegalStateException ("Refresh token was generated but not saved.");
                }

                return LoginResponse.builder()
                        .accessToken(accessToken)
                        .refreshToken(refreshToken)
                        .build();
        }

        @Override
        public RefreshResponse getNewAccessToken(@NonNull RefreshRequest refreshRequest) {

                if (!jwtService.isRefreshTokenValid(refreshRequest.getRefreshToken())) {
                        log.error(String.format("⚠️ Invalid JWT refresh token: %s", refreshRequest.getRefreshToken()));
                        throw new BadCredentialsException("Invalid JWT refresh token. Please, login.");
                }

                String email = jwtService.getUserEmailFromRefreshToken(refreshRequest.getRefreshToken());
                final String accessToken = jwtService.generateAccessToken(email);

                return new RefreshResponse(accessToken);
        }
}
