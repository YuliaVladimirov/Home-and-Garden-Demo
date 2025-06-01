package org.example.homeandgarden.user.service;

import org.example.homeandgarden.shared.MessageResponse;
import org.example.homeandgarden.user.dto.*;
import org.example.homeandgarden.exception.*;
import lombok.RequiredArgsConstructor;
import org.example.homeandgarden.user.entity.enums.UserRole;
import org.example.homeandgarden.user.entity.User;
import org.example.homeandgarden.user.mapper.UserMapper;
import org.example.homeandgarden.user.repository.UserRepository;
import org.springframework.data.web.PagedModel;
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
    public PagedModel<UserResponse> getAllUsers(Boolean isEnabled, Integer size, Integer page, String order, String sortBy) {
        Pageable pageRequest = PageRequest.of(page, size, Sort.Direction.fromString(order), sortBy);
        return new PagedModel<>(userRepository.findAllByIsEnabled(isEnabled, pageRequest).map(userMapper::userToUserResponse));
    }

    @Override
    public UserResponse getUserById(String userId) {
        UUID id = UUID.fromString(userId);
        User existingUser = userRepository.findById(id).orElseThrow(() -> new DataNotFoundException (String.format("User with id: %s, was not found.", userId)));
        return userMapper.userToUserResponse(existingUser);
    }

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

        return userMapper.userToUserResponse(registeredUser);
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

        return userMapper.userToUserResponse(updatedUser);
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

        if (!promotedUser.getUserRole().equals(role)) {
            throw new IllegalStateException(String.format("Unfortunately something went wrong and userRole '%s' was not set for user with id: %s. Please, try again.", userRole, userId));
        }

        return MessageResponse.builder()
                .message(String.format("UserRole %s was set for user with id: %s.", userRole, userId))
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

        if (existingUser.getIsNonLocked()) {
            existingUser.setIsNonLocked(Boolean.FALSE);
            User lockedUser = userRepository.saveAndFlush(existingUser);

            if (lockedUser.getIsNonLocked()) {
                throw new IllegalStateException(String.format("Unfortunately something went wrong and user with id: %s, was not locked. Please, try again.", userId));
            }

            return MessageResponse.builder()
                    .message(String.format("User with id: %s, has been locked.", userId))
                    .build();

        } else {
            existingUser.setIsNonLocked(Boolean.TRUE);
            User unlockedUser = userRepository.saveAndFlush(existingUser);

            if (!unlockedUser.getIsNonLocked()) {
                throw new IllegalStateException(String.format("Unfortunately something went wrong and user with id: %s, was not unlocked. Please, try again.", userId));
            }
            return MessageResponse.builder()
                    .message(String.format("User with id: %s, has been unlocked.", userId))
                    .build();
        }
    }

    @Override
    @Transactional
    public MessageResponse unregisterUser(String userId, UserUnregisterRequest userUnregisterRequest) {

        if (!userUnregisterRequest.getPassword().equals(userUnregisterRequest.getConfirmPassword())) {
            throw new BadCredentialsException("Password doesn't match the CONFIRM PASSWORD field.");
        }

        UUID id = UUID.fromString(userId);
        User existingUser = userRepository.findById(id).orElseThrow(() -> new DataNotFoundException(String.format("User with id: %s, was not found.", userId)));

        if (!passwordEncoder.matches(userUnregisterRequest.getPassword(), existingUser.getPasswordHash())) {
            throw new BadCredentialsException("Given password doesn't match the password saved in database.");
        }
        if (!existingUser.getIsNonLocked()) {
            throw new IllegalArgumentException(String.format("User with id: %s, is locked and can not be unregistered.", userId));
        }
        if (!existingUser.getIsEnabled()) {
            throw new IllegalArgumentException(String.format("User with id: %s, is already unregistered.", userId));
        }

        existingUser.setEmail(String.format("%s@example.com", existingUser.getUserId()));
        existingUser.setFirstName("Deleted User");
        existingUser.setLastName("Deleted User");
        existingUser.setIsEnabled(Boolean.FALSE);
        User unregisteredUser = userRepository.saveAndFlush(existingUser);

        if (unregisteredUser.getIsEnabled()){
            throw new IllegalStateException(String.format("Unfortunately something went wrong and user with id: %s, was not unregistered. Please, try again.", userId));
        }

        return MessageResponse.builder()
                .message(String.format("User with id: %s, has been unregistered.", userId))
                .build();
    }
}