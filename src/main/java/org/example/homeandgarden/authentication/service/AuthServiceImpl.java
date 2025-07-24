package org.example.homeandgarden.authentication.service;

import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.example.homeandgarden.exception.DataAlreadyExistsException;
import org.example.homeandgarden.exception.DataNotFoundException;
import org.example.homeandgarden.security.config.JwtService;
import org.example.homeandgarden.authentication.dto.LoginRequest;
import org.example.homeandgarden.authentication.dto.LoginResponse;
import org.example.homeandgarden.authentication.dto.RefreshRequest;
import org.example.homeandgarden.authentication.dto.RefreshResponse;
import org.example.homeandgarden.user.dto.UserRegisterRequest;
import org.example.homeandgarden.user.dto.UserResponse;
import org.example.homeandgarden.user.entity.User;
import org.example.homeandgarden.user.mapper.UserMapper;
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
public class AuthServiceImpl implements AuthService {

    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;
    private final UserRepository userRepository;
    private final UserMapper userMapper;

    @Override
    @Transactional
    public UserResponse registerUser(UserRegisterRequest userRegisterRequest) {

        if (!userRegisterRequest.getPassword().equals(userRegisterRequest.getConfirmPassword())) {
            throw new BadCredentialsException("Password doesn't match the CONFIRM PASSWORD field.");
        }
        if (userRepository.existsByEmail(userRegisterRequest.getEmail())) {
            if (userRepository.existsByEmailAndIsEnabledFalse(userRegisterRequest.getEmail())) {
                throw new DataAlreadyExistsException(String.format("User with email: %s, already exists and is disabled.", userRegisterRequest.getEmail()));
            }
            if (userRepository.existsByEmailAndIsNonLockedFalse(userRegisterRequest.getEmail())) {
                throw new DataAlreadyExistsException(String.format("User with email: %s, already exists and is locked.", userRegisterRequest.getEmail()));
            }
            throw new DataAlreadyExistsException(String.format("User with email: %s, already registered.", userRegisterRequest.getEmail()));
        }

        User userToRegister = userMapper.createRequestToUser(userRegisterRequest);
        User registeredUser = userRepository.saveAndFlush(userToRegister);

        return userMapper.userToResponse(registeredUser);
    }

    @Override
    @Transactional
    public LoginResponse login(LoginRequest loginRequest) {

        Authentication authentication;
        try {
            authentication = authenticationManager
                    .authenticate(new UsernamePasswordAuthenticationToken(loginRequest.getEmail(), loginRequest.getPassword()));
        } catch (AuthenticationException exception) {
            throw new BadCredentialsException("Invalid email or password.");
        }

        SecurityContextHolder.getContext().setAuthentication(authentication);

        UserDetails userDetails = (UserDetails) authentication.getPrincipal();
        final String accessToken = jwtService.generateAccessToken(userDetails.getUsername());
        final String refreshToken = jwtService.generateRefreshToken(userDetails.getUsername());

        User logedInUser = userRepository.findByEmail(userDetails.getUsername()).orElseThrow(() -> new DataNotFoundException(String.format("User with email: %s, was not found.", userDetails.getUsername())));

        logedInUser.setRefreshToken(refreshToken);
        userRepository.saveAndFlush(logedInUser);

        return LoginResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .build();
    }

    @Override
    public RefreshResponse getNewAccessToken(@NonNull RefreshRequest refreshRequest) {

        if (refreshRequest.getRefreshToken() == null || refreshRequest.getRefreshToken().isBlank()) {
            throw new BadCredentialsException("Refresh token is missing or empty. Please log in again.");
        }

        final String refreshToken = refreshRequest.getRefreshToken();
        if (!jwtService.isRefreshTokenValid(refreshToken)) {
            throw new BadCredentialsException("Invalid or expired refresh token. Please log in again.");
        }

        String email = jwtService.getUserEmailFromRefreshToken(refreshToken);
        if (email == null) {
            throw new BadCredentialsException("Token does not contain a user identifier. Please log in again.");
        }

        User user = userRepository.findByEmail(email).orElseThrow(() -> new DataNotFoundException("User associated with refresh token not found. Please log in again."));

        if (!refreshToken.equals(user.getRefreshToken())) {
            user.setRefreshToken(null);
            userRepository.saveAndFlush(user);
            throw new BadCredentialsException("Refresh token mismatch or reuse detected. Please log in again.");
        }

        final String newAccessToken = jwtService.generateAccessToken(email);

        return RefreshResponse.builder()
                .accessToken(newAccessToken)
                .build();
    }
}
