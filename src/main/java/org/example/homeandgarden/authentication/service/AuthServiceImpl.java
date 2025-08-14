package org.example.homeandgarden.authentication.service;

import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.example.homeandgarden.authentication.dto.*;
import org.example.homeandgarden.email.service.EmailService;
import org.example.homeandgarden.exception.DataAlreadyExistsException;
import org.example.homeandgarden.exception.DataNotFoundException;
import org.example.homeandgarden.security.config.JwtService;
import org.example.homeandgarden.shared.MessageResponse;
import org.example.homeandgarden.user.dto.UserRegisterRequest;
import org.example.homeandgarden.user.dto.UserResponse;
import org.example.homeandgarden.user.entity.User;
import org.example.homeandgarden.user.mapper.UserMapper;
import org.example.homeandgarden.user.repository.UserRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;
    private final UserRepository userRepository;
    private final UserMapper userMapper;
    private final EmailService emailService;
    private final PasswordEncoder passwordEncoder;

    @Value("${app.password-reset.base-url}")
    private String resetBaseUrl;

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
        userToRegister.setPasswordHash(passwordEncoder.encode(userRegisterRequest.getPassword()));
        User registeredUser = userRepository.save(userToRegister);

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
            throw new BadCredentialsException("Invalid email or password. Please log in again.");
        }

        SecurityContextHolder.getContext().setAuthentication(authentication);

        UserDetails userDetails = (UserDetails) authentication.getPrincipal();
        final String accessToken = jwtService.generateAccessToken(userDetails.getUsername());
        final String refreshToken = jwtService.generateRefreshToken(userDetails.getUsername());

        User logedInUser = userRepository.findByEmail(userDetails.getUsername()).orElseThrow(() -> new DataNotFoundException(String.format("User with email: %s, was not found.", userDetails.getUsername())));

        logedInUser.setRefreshToken(refreshToken);
        userRepository.save(logedInUser);

        return LoginResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .build();
    }

    @Override
    @Transactional
    public RefreshResponse getNewAccessToken(@NonNull RefreshRequest refreshRequest) {

        if (refreshRequest.getRefreshToken() == null || refreshRequest.getRefreshToken().isBlank()) {
            throw new BadCredentialsException("Refresh token is missing or empty. Please log in.");
        }

        final String refreshToken = refreshRequest.getRefreshToken();
        if (!jwtService.isRefreshTokenValid(refreshToken)) {
            throw new BadCredentialsException("Invalid or expired refresh token. Please log in.");
        }

        String email = jwtService.getUserEmailFromRefreshToken(refreshToken);
        if (email == null) {
            throw new BadCredentialsException("Token does not contain a user identifier. Please log in.");
        }

        User existingUser = userRepository.findByEmail(email).orElseThrow(() -> new DataNotFoundException(String.format("User with email: %s, associated with refresh token, not found. Please log in.", email)));

        if (!existingUser.getIsEnabled()) {
            throw new IllegalArgumentException(String.format("User with email: %s, is unregistered.", email));
        }
        if (!existingUser.getIsNonLocked()) {
            throw new IllegalArgumentException(String.format("User with email: %s, is locked.", email));
        }

        if (!refreshToken.equals(existingUser.getRefreshToken())) {
            existingUser.setRefreshToken(null);
            userRepository.save(existingUser);
            throw new BadCredentialsException("Refresh token mismatch or reuse detected. Please log in.");
        }

        final String newAccessToken = jwtService.generateAccessToken(email);

        return RefreshResponse.builder()
                .accessToken(newAccessToken)
                .build();
    }

    @Override
    @Transactional
    public MessageResponse forgotPassword(ForgotPasswordRequest request) {

        User existingUser = userRepository.findByEmail(request.getEmail()).orElseThrow(() -> new DataNotFoundException(String.format("User with email: %s, was not found.", request.getEmail())));

        if (!existingUser.getIsEnabled()) {
            throw new IllegalArgumentException(String.format("User with email: %s, is unregistered.", existingUser.getEmail()));
        }
        if (!existingUser.getIsNonLocked()) {
            throw new IllegalArgumentException(String.format("User with email: %s, is locked.", existingUser.getEmail()));
        }

        String resetToken = jwtService.generatePasswordResetToken(existingUser.getEmail());

        existingUser.setPasswordResetToken(resetToken);
        userRepository.save(existingUser);

        String resetLink = resetBaseUrl + "?token=" + resetToken;
        emailService.sendPasswordResetEmail(existingUser.getEmail(), "Password Reset Request", resetLink);

        return MessageResponse.builder()
                .message("Password reset link sent to user's email.")
                .build();

    }

    @Override
    @Transactional
    public MessageResponse resetPassword(PasswordResetRequest resetRequest) {

        if (!resetRequest.getNewPassword().equals(resetRequest.getConfirmPassword())) {
            throw new BadCredentialsException("Password doesn't match the CONFIRM PASSWORD field. Please try resetting your password again");
        }

        if (resetRequest.getPasswordResetToken() == null || resetRequest.getPasswordResetToken().isBlank()) {
            throw new BadCredentialsException("Password reset token is missing or empty. Please try resetting your password again.");
        }

        final String passwordResetToken = resetRequest.getPasswordResetToken();
        if (!jwtService.isPasswordResetTokenValid(passwordResetToken)) {
            throw new BadCredentialsException("Invalid or expired password reset token. Please try resetting your password again.");
        }

        String email = jwtService.getUserEmailFromPasswordResetToken(passwordResetToken);
        if (email == null) {
            throw new BadCredentialsException("Token does not contain a user identifier. Please try resetting your password again.");
        }

        User existingUser = userRepository.findByEmail(email).orElseThrow(() -> new DataNotFoundException(String.format("User with email: %s, associated with password reset token, not found.", email)));

        if (!existingUser.getIsEnabled()) {
            throw new IllegalArgumentException(String.format("User with email: %s, is unregistered.", email));
        }
        if (!existingUser.getIsNonLocked()) {
            throw new IllegalArgumentException(String.format("User with email: %s, is locked.", email));
        }

        if (!passwordResetToken.equals(existingUser.getPasswordResetToken())) {
            existingUser.setPasswordResetToken(null);
            userRepository.save(existingUser);
            throw new BadCredentialsException("Password reset token mismatch or reuse detected. Please try resetting your password again.");
        }

        existingUser.setPasswordHash(passwordEncoder.encode(resetRequest.getNewPassword()));
        existingUser.setPasswordResetToken(null);
        userRepository.save(existingUser);

        return MessageResponse.builder()
                .message("Password has been successfully reset.")
                .build();
    }
}
