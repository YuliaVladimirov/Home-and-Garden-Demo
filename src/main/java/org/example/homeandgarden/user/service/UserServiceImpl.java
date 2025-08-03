package org.example.homeandgarden.user.service;

import org.example.homeandgarden.shared.MessageResponse;
import org.example.homeandgarden.user.dto.*;
import org.example.homeandgarden.exception.*;
import lombok.RequiredArgsConstructor;
import org.example.homeandgarden.user.entity.enums.UserRole;
import org.example.homeandgarden.user.entity.User;
import org.example.homeandgarden.user.mapper.UserMapper;
import org.example.homeandgarden.user.repository.UserRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;


@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final UserMapper userMapper;

    @Override
    public Page<UserResponse> getUsersByStatus(Boolean isEnabled, Boolean isNonLocked, Integer size, Integer page, String order, String sortBy) {
        Pageable pageRequest = PageRequest.of(page, size, Sort.Direction.fromString(order), sortBy);
        return userRepository.findAllByIsEnabledAndIsNonLocked(isEnabled, isNonLocked, pageRequest).map(userMapper::userToResponseDetailed);
    }

    @Override
    public UserResponse getUserById(String userId) {
        UUID id = UUID.fromString(userId);
        User existingUser = userRepository.findById(id).orElseThrow(() -> new DataNotFoundException (String.format("User with id: %s, was not found.", userId)));
        return userMapper.userToResponse(existingUser);
    }

    @Override
    public UserResponse getMyProfile(String userEmail) {

        User existingUser = userRepository.findByEmail(userEmail).orElseThrow(() -> new DataNotFoundException (String.format("User with email: %s, was not found.", userEmail)));
        return userMapper.userToResponse(existingUser);
    }

    @Override
    @Transactional
    public UserResponse updateUser(String userId, UserUpdateRequest userUpdateRequest) {

        UUID id = UUID.fromString(userId);
        User existingUser = userRepository.findById(id).orElseThrow(() -> new DataNotFoundException (String.format("User with id: %s, was not found.", userId)));

        if (!existingUser.getIsEnabled()) {
            throw new IllegalArgumentException(String.format("User with id: %s, is unregistered and can not be updated.", userId));
        }
        if (!existingUser.getIsNonLocked()) {
            throw new IllegalArgumentException(String.format("User with id: %s, is locked and can not be updated.", userId));
        }

        Optional.ofNullable(userUpdateRequest.getFirstName()).ifPresent(existingUser::setFirstName);
        Optional.ofNullable(userUpdateRequest.getLastName()).ifPresent(existingUser::setLastName);

        User updatedUser = userRepository.saveAndFlush(existingUser);

        return userMapper.userToResponse(updatedUser);
    }

    @Override
    @Transactional
    public MessageResponse setUserRole(String userId, String userRole) {

        UUID id = UUID.fromString(userId);
        User existingUser = userRepository.findById(id).orElseThrow(() -> new DataNotFoundException (String.format("User with id: %s, was not found.", userId)));

        UserRole role = UserRole.valueOf(userRole.toUpperCase());

        if (!existingUser.getIsNonLocked()) {
            throw new IllegalArgumentException(String.format("User with id: %s, is locked and his userRole can not be changed.", userId));
        }
        if (!existingUser.getIsEnabled()) {
            throw new IllegalArgumentException(String.format("User with id: %s, is unregistered and his userRole can not be changed.", userId));
        }
        if (existingUser.getUserRole().equals(role)) {
            throw new IllegalArgumentException(String.format("User with id: %s, already has userRole '%s'.", userId, userRole));
        }
        existingUser.setUserRole(role);
        User promotedUser = userRepository.saveAndFlush(existingUser);

        return MessageResponse.builder()
                .message(String.format("UserRole %s was set for user with id: %s.", promotedUser.getUserRole().name(), promotedUser.getUserId().toString()))
                .build();
    }

    @Override
    @Transactional
    public MessageResponse toggleLockState(String userId) {

        UUID id = UUID.fromString(userId);
        User existingUser = userRepository.findById(id).orElseThrow(() -> new DataNotFoundException (String.format("User with id: %s, was not found.", userId)));

        if (!existingUser.getIsEnabled()) {
            throw new IllegalArgumentException(String.format("User with id: %s, is unregistered and can not be locked or unlocked.", userId));
        }

        Boolean lockState = existingUser.getIsNonLocked();
        existingUser.setIsNonLocked(!lockState);

        User lockedUser = userRepository.saveAndFlush(existingUser);
        return MessageResponse.builder()
                .message(String.format("User with id: %s has been %s.", lockedUser.getUserId().toString(), lockState ? "locked" : "unlocked"))
                .build();
    }

    @Override
    @Transactional
    public MessageResponse unregisterMyAccount(String email, UserUnregisterRequest userUnregisterRequest) {

        if (!userUnregisterRequest.getPassword().equals(userUnregisterRequest.getConfirmPassword())) {
            throw new BadCredentialsException("Password doesn't match the CONFIRM PASSWORD field.");
        }

        User existingUser = userRepository.findByEmail(email).orElseThrow(() -> new DataNotFoundException(String.format("User with email: %s, was not found.", email)));

        if (!passwordEncoder.matches(userUnregisterRequest.getPassword(), existingUser.getPasswordHash())) {
            throw new BadCredentialsException("Given password doesn't match the password saved in database.");
        }

        existingUser.setEmail(String.format("%s@example.com", existingUser.getUserId()));
        existingUser.setFirstName("Disabled User");
        existingUser.setLastName("Disabled User");
        existingUser.setRefreshToken(null);
        existingUser.setIsEnabled(Boolean.FALSE);
        userRepository.saveAndFlush(existingUser);

        return MessageResponse.builder()
                .message(String.format("User with email: %s, has been unregistered.", email))
                .build();
    }

    @Override
    public MessageResponse changeMyPassword(String email, ChangePasswordRequest changePasswordRequest) {

        if (!changePasswordRequest.getNewPassword().equals(changePasswordRequest.getConfirmNewPassword())) {
            throw new BadCredentialsException("Password doesn't match the CONFIRM NEW PASSWORD field.");
        }

        User existingUser = userRepository.findByEmail(email).orElseThrow(() -> new DataNotFoundException(String.format("User with email: %s, was not found.", email)));

        if (!passwordEncoder.matches(changePasswordRequest.getCurrentPassword(), existingUser.getPasswordHash())) {
            throw new BadCredentialsException("Given current password doesn't match the one in database.");
        }

        existingUser.setPasswordHash(passwordEncoder.encode(changePasswordRequest.getNewPassword()));
        User updatedUser = userRepository.saveAndFlush(existingUser);

        return MessageResponse.builder()
                .message(String.format("Password for user with email: %s, has been successfully changed.", updatedUser.getEmail()))
                .build();
    }
}