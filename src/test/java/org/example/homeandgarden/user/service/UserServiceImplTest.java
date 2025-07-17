package org.example.homeandgarden.user.service;

import org.example.homeandgarden.exception.DataNotFoundException;
import org.example.homeandgarden.shared.MessageResponse;
import org.example.homeandgarden.user.dto.UserResponse;
import org.example.homeandgarden.user.dto.UserUnregisterRequest;
import org.example.homeandgarden.user.dto.UserUpdateRequest;
import org.example.homeandgarden.user.entity.User;
import org.example.homeandgarden.user.entity.enums.UserRole;
import org.example.homeandgarden.user.mapper.UserMapper;
import org.example.homeandgarden.user.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.*;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceImplTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private UserMapper userMapper;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private UserServiceImpl userService;

    private final Integer PAGE = 0;
    private final Integer SIZE = 5;
    private final String ORDER = "ASC";
    private final String SORT_BY = "createdAt";

    private final UUID USER_1_ID = UUID.randomUUID();
    private final UUID USER_2_ID = UUID.randomUUID();

    private final UUID USER_ID = UUID.randomUUID();
    private final UUID NON_EXISTING_USER_ID = UUID.randomUUID();

    private final String INVALID_ID = "Invalid UUID";

    private final UserRole USER_ROLE_CLIENT = UserRole.CLIENT;
    private final UserRole USER_ROLE_ADMIN = UserRole.ADMINISTRATOR;

    private final String PASSWORD = "Raw Password";
    private final String PASSWORD_HASH = "Hashed Password";

    private final Instant TIMESTAMP_NOW = Instant.now();
    private final Instant TIMESTAMP_PAST = Instant.now().minus(10L, ChronoUnit.DAYS);

    @Test
    void getUsersByStatus_shouldReturnPagedModelOfEnabledAndNonLockedUsers() {

        Boolean isEnabled = true;
        Boolean isNonLocked = true;

        Pageable pageRequest = PageRequest.of(PAGE, SIZE, Sort.Direction.fromString(ORDER), SORT_BY);

        User user1 = User.builder()
                .userId(USER_1_ID)
                .email("Email One")
                .passwordHash("Hashed Password One")
                .firstName("First Name One")
                .lastName("Last Name One")
                .userRole(USER_ROLE_CLIENT)
                .isEnabled(true)
                .isNonLocked(true)
                .registeredAt(TIMESTAMP_PAST)
                .updatedAt(TIMESTAMP_PAST)
                .build();

        User user2 = User.builder()
                .userId(USER_2_ID)
                .email("Email Two")
                .passwordHash("Hashed Password Two")
                .firstName("First Name Two")
                .lastName("Last Name Two")
                .userRole(USER_ROLE_CLIENT)
                .isEnabled(true)
                .isNonLocked(true)
                .registeredAt(TIMESTAMP_PAST)
                .updatedAt(TIMESTAMP_PAST)
                .build();

        List<User> allUsers = List.of(user1, user2);
        Page<User> userPage = new PageImpl<>(allUsers, pageRequest, allUsers.size());
        long expectedTotalPages = (long) Math.ceil((double) allUsers.size() / SIZE);

        UserResponse userResponseDetailed1 = UserResponse.builder()
                .userId(user1.getUserId())
                .email(user1.getEmail())
                .firstName(user1.getFirstName())
                .lastName(user1.getLastName())
                .userRole(user1.getUserRole())
                .isEnabled(user1.getIsEnabled())
                .isNonLocked(user1.getIsNonLocked())
                .registeredAt(user1.getRegisteredAt())
                .updatedAt(user1.getUpdatedAt())
                .build();

        UserResponse userResponseDetailed2 = UserResponse.builder()
                .userId(user2.getUserId())
                .email(user2.getEmail())
                .firstName(user2.getFirstName())
                .lastName(user2.getLastName())
                .userRole(user2.getUserRole())
                .isEnabled(user2.getIsEnabled())
                .isNonLocked(user2.getIsNonLocked())
                .registeredAt(user2.getRegisteredAt())
                .updatedAt(user2.getUpdatedAt())
                .build();

        when(userRepository.findAllByIsEnabledAndIsNonLocked(isEnabled, isNonLocked, pageRequest)).thenReturn(userPage);
        when(userMapper.userToResponseDetailed(user1)).thenReturn(userResponseDetailed1);
        when(userMapper.userToResponseDetailed(user2)).thenReturn(userResponseDetailed2);

        Page<UserResponse> actualResponse = userService.getUsersByStatus(isEnabled, isNonLocked, SIZE, PAGE, ORDER, SORT_BY);

        verify(userRepository, times(1)).findAllByIsEnabledAndIsNonLocked(isEnabled, isNonLocked, pageRequest);
        verify(userMapper, times(1)).userToResponseDetailed(user1);
        verify(userMapper, times(1)).userToResponseDetailed(user2);

        assertNotNull(actualResponse);
        assertNotNull(actualResponse.getContent());
        assertEquals(allUsers.size(), actualResponse.getTotalElements());
        assertEquals(expectedTotalPages, actualResponse.getTotalPages());
        assertEquals((long)SIZE, actualResponse.getSize());
        assertEquals((long)PAGE, actualResponse.getNumber());

        assertNotNull(actualResponse.getContent());
        assertEquals(allUsers.size(), actualResponse.getContent().size());
        assertEquals(userResponseDetailed1.getUserId(), actualResponse.getContent().getFirst().getUserId());
        assertEquals(userResponseDetailed1.getEmail(), actualResponse.getContent().getFirst().getEmail());
        assertEquals(userResponseDetailed1.getFirstName(), actualResponse.getContent().getFirst().getFirstName());
        assertEquals(userResponseDetailed1.getLastName(), actualResponse.getContent().getFirst().getLastName());
        assertEquals(userResponseDetailed1.getUserRole(), actualResponse.getContent().getFirst().getUserRole());
        assertEquals(isEnabled, actualResponse.getContent().getFirst().getIsEnabled());
        assertEquals(isNonLocked, actualResponse.getContent().getFirst().getIsNonLocked());


        assertEquals(userResponseDetailed2.getUserId(), actualResponse.getContent().get(1).getUserId());
        assertEquals(userResponseDetailed2.getEmail(), actualResponse.getContent().get(1).getEmail());
        assertEquals(userResponseDetailed2.getFirstName(), actualResponse.getContent().get(1).getFirstName());
        assertEquals(userResponseDetailed2.getLastName(), actualResponse.getContent().get(1).getLastName());
        assertEquals(userResponseDetailed2.getUserRole(), actualResponse.getContent().get(1).getUserRole());
        assertEquals(isEnabled, actualResponse.getContent().get(1).getIsEnabled());
        assertEquals(isNonLocked, actualResponse.getContent().get(1).getIsNonLocked());
    }

    @Test
    void getUsersByStatus_shouldReturnPagedModelOfDisabledButNonLockedUsers() {

        Boolean isEnabled = false;
        Boolean isNonLocked = true;

        Pageable pageRequest = PageRequest.of(PAGE, SIZE, Sort.Direction.fromString(ORDER), SORT_BY);

        User user1 = User.builder()
                .userId(USER_1_ID)
                .email("Email One")
                .passwordHash("Hashed Password One")
                .firstName("First Name One")
                .lastName("Last Name One")
                .userRole(USER_ROLE_CLIENT)
                .isEnabled(false)
                .isNonLocked(true)
                .registeredAt(TIMESTAMP_PAST)
                .updatedAt(TIMESTAMP_PAST)
                .build();

        User user2 = User.builder()
                .userId(USER_2_ID)
                .email("Email Two")
                .passwordHash("Hashed Password Two")
                .firstName("First Name Two")
                .lastName("Last Name Two")
                .userRole(USER_ROLE_CLIENT)
                .isEnabled(false)
                .isNonLocked(true)
                .registeredAt(TIMESTAMP_PAST)
                .updatedAt(TIMESTAMP_PAST)
                .build();

        List<User> allUsers = List.of(user1, user2);
        Page<User> userPage = new PageImpl<>(allUsers, pageRequest, allUsers.size());
        long expectedTotalPages = (long) Math.ceil((double) allUsers.size() / SIZE);

        UserResponse userResponseDetailed1 = UserResponse.builder()
                .userId(user1.getUserId())
                .email(user1.getEmail())
                .firstName(user1.getFirstName())
                .lastName(user1.getLastName())
                .userRole(user1.getUserRole())
                .isEnabled(user1.getIsEnabled())
                .isNonLocked(user1.getIsNonLocked())
                .registeredAt(user1.getRegisteredAt())
                .updatedAt(user1.getUpdatedAt())
                .build();

        UserResponse userResponseDetailed2 = UserResponse.builder()
                .userId(user2.getUserId())
                .email(user2.getEmail())
                .firstName(user2.getFirstName())
                .lastName(user2.getLastName())
                .userRole(user2.getUserRole())
                .isEnabled(user2.getIsEnabled())
                .isNonLocked(user2.getIsNonLocked())
                .registeredAt(user2.getRegisteredAt())
                .updatedAt(user2.getUpdatedAt())
                .build();

        when(userRepository.findAllByIsEnabledAndIsNonLocked(isEnabled, isNonLocked, pageRequest)).thenReturn(userPage);
        when(userMapper.userToResponseDetailed(user1)).thenReturn(userResponseDetailed1);
        when(userMapper.userToResponseDetailed(user2)).thenReturn(userResponseDetailed2);

        Page<UserResponse> actualResponse = userService.getUsersByStatus(isEnabled, isNonLocked, SIZE, PAGE, ORDER, SORT_BY);

        verify(userRepository, times(1)).findAllByIsEnabledAndIsNonLocked(isEnabled, isNonLocked, pageRequest);
        verify(userMapper, times(1)).userToResponseDetailed(user1);
        verify(userMapper, times(1)).userToResponseDetailed(user2);

        assertNotNull(actualResponse);
        assertNotNull(actualResponse.getContent());
        assertEquals(allUsers.size(), actualResponse.getTotalElements());
        assertEquals(expectedTotalPages, actualResponse.getTotalPages());
        assertEquals((long)SIZE, actualResponse.getSize());
        assertEquals((long)PAGE, actualResponse.getNumber());

        assertNotNull(actualResponse.getContent());
        assertEquals(allUsers.size(), actualResponse.getContent().size());
        assertEquals(userResponseDetailed1.getUserId(), actualResponse.getContent().getFirst().getUserId());
        assertEquals(userResponseDetailed1.getEmail(), actualResponse.getContent().getFirst().getEmail());
        assertEquals(userResponseDetailed1.getFirstName(), actualResponse.getContent().getFirst().getFirstName());
        assertEquals(userResponseDetailed1.getLastName(), actualResponse.getContent().getFirst().getLastName());
        assertEquals(userResponseDetailed1.getUserRole(), actualResponse.getContent().getFirst().getUserRole());
        assertEquals(isEnabled, actualResponse.getContent().getFirst().getIsEnabled());
        assertEquals(isNonLocked, actualResponse.getContent().getFirst().getIsNonLocked());


        assertEquals(userResponseDetailed2.getUserId(), actualResponse.getContent().get(1).getUserId());
        assertEquals(userResponseDetailed2.getEmail(), actualResponse.getContent().get(1).getEmail());
        assertEquals(userResponseDetailed2.getFirstName(), actualResponse.getContent().get(1).getFirstName());
        assertEquals(userResponseDetailed2.getLastName(), actualResponse.getContent().get(1).getLastName());
        assertEquals(userResponseDetailed2.getUserRole(), actualResponse.getContent().get(1).getUserRole());
        assertEquals(isEnabled, actualResponse.getContent().get(1).getIsEnabled());
        assertEquals(isNonLocked, actualResponse.getContent().get(1).getIsNonLocked());
    }

    @Test
    void getUsersByStatus_shouldReturnPagedModelOfEnabledButLockedUsers() {

        Boolean isEnabled = true;
        Boolean isNonLocked = false;

        Pageable pageRequest = PageRequest.of(PAGE, SIZE, Sort.Direction.fromString(ORDER), SORT_BY);

        User user1 = User.builder()
                .userId(USER_1_ID)
                .email("Email One")
                .passwordHash("Hashed Password One")
                .firstName("First Name One")
                .lastName("Last Name One")
                .userRole(USER_ROLE_CLIENT)
                .isEnabled(true)
                .isNonLocked(false)
                .registeredAt(TIMESTAMP_PAST)
                .updatedAt(TIMESTAMP_PAST)
                .build();

        User user2 = User.builder()
                .userId(USER_2_ID)
                .email("Email Two")
                .passwordHash("Hashed Password Two")
                .firstName("First Name Two")
                .lastName("Last Name Two")
                .userRole(USER_ROLE_CLIENT)
                .isEnabled(true)
                .isNonLocked(false)
                .registeredAt(TIMESTAMP_PAST)
                .updatedAt(TIMESTAMP_PAST)
                .build();

        List<User> allUsers = List.of(user1, user2);
        Page<User> userPage = new PageImpl<>(allUsers, pageRequest, allUsers.size());
        long expectedTotalPages = (long) Math.ceil((double) allUsers.size() / SIZE);

        UserResponse userResponseDetailed1 = UserResponse.builder()
                .userId(user1.getUserId())
                .email(user1.getEmail())
                .firstName(user1.getFirstName())
                .lastName(user1.getLastName())
                .userRole(user1.getUserRole())
                .isEnabled(user1.getIsEnabled())
                .isNonLocked(user1.getIsNonLocked())
                .registeredAt(user1.getRegisteredAt())
                .updatedAt(user1.getUpdatedAt())
                .build();

        UserResponse userResponseDetailed2 = UserResponse.builder()
                .userId(user2.getUserId())
                .email(user2.getEmail())
                .firstName(user2.getFirstName())
                .lastName(user2.getLastName())
                .userRole(user2.getUserRole())
                .isEnabled(user2.getIsEnabled())
                .isNonLocked(user2.getIsNonLocked())
                .registeredAt(user2.getRegisteredAt())
                .updatedAt(user2.getUpdatedAt())
                .build();

        when(userRepository.findAllByIsEnabledAndIsNonLocked(isEnabled, isNonLocked, pageRequest)).thenReturn(userPage);
        when(userMapper.userToResponseDetailed(user1)).thenReturn(userResponseDetailed1);
        when(userMapper.userToResponseDetailed(user2)).thenReturn(userResponseDetailed2);

        Page<UserResponse> actualResponse = userService.getUsersByStatus(isEnabled, isNonLocked, SIZE, PAGE, ORDER, SORT_BY);

        verify(userRepository, times(1)).findAllByIsEnabledAndIsNonLocked(isEnabled, isNonLocked, pageRequest);
        verify(userMapper, times(1)).userToResponseDetailed(user1);
        verify(userMapper, times(1)).userToResponseDetailed(user2);

        assertNotNull(actualResponse);
        assertNotNull(actualResponse.getContent());
        assertEquals(allUsers.size(), actualResponse.getTotalElements());
        assertEquals(expectedTotalPages, actualResponse.getTotalPages());
        assertEquals((long)SIZE, actualResponse.getSize());
        assertEquals((long)PAGE, actualResponse.getNumber());

        assertNotNull(actualResponse.getContent());
        assertEquals(allUsers.size(), actualResponse.getContent().size());
        assertEquals(userResponseDetailed1.getUserId(), actualResponse.getContent().getFirst().getUserId());
        assertEquals(userResponseDetailed1.getEmail(), actualResponse.getContent().getFirst().getEmail());
        assertEquals(userResponseDetailed1.getFirstName(), actualResponse.getContent().getFirst().getFirstName());
        assertEquals(userResponseDetailed1.getLastName(), actualResponse.getContent().getFirst().getLastName());
        assertEquals(userResponseDetailed1.getUserRole(), actualResponse.getContent().getFirst().getUserRole());
        assertEquals(isEnabled, actualResponse.getContent().getFirst().getIsEnabled());
        assertEquals(isNonLocked, actualResponse.getContent().getFirst().getIsNonLocked());


        assertEquals(userResponseDetailed2.getUserId(), actualResponse.getContent().get(1).getUserId());
        assertEquals(userResponseDetailed2.getEmail(), actualResponse.getContent().get(1).getEmail());
        assertEquals(userResponseDetailed2.getFirstName(), actualResponse.getContent().get(1).getFirstName());
        assertEquals(userResponseDetailed2.getLastName(), actualResponse.getContent().get(1).getLastName());
        assertEquals(userResponseDetailed2.getUserRole(), actualResponse.getContent().get(1).getUserRole());
        assertEquals(isEnabled, actualResponse.getContent().get(1).getIsEnabled());
        assertEquals(isNonLocked, actualResponse.getContent().get(1).getIsNonLocked());
    }

    @Test
    void getUsersByStatus_shouldReturnPagedModelOfDisabledAndLockedUsers() {

        Boolean isEnabled = false;
        Boolean isNonLocked = false;

        Pageable pageRequest = PageRequest.of(PAGE, SIZE, Sort.Direction.fromString(ORDER), SORT_BY);

        User user1 = User.builder()
                .userId(USER_1_ID)
                .email("Email One")
                .passwordHash("Hashed Password One")
                .firstName("First Name One")
                .lastName("Last Name One")
                .userRole(USER_ROLE_CLIENT)
                .isEnabled(false)
                .isNonLocked(false)
                .registeredAt(TIMESTAMP_PAST)
                .updatedAt(TIMESTAMP_PAST)
                .build();

        User user2 = User.builder()
                .userId(USER_2_ID)
                .email("Email Two")
                .passwordHash("Hashed Password Two")
                .firstName("First Name Two")
                .lastName("Last Name Two")
                .userRole(USER_ROLE_CLIENT)
                .isEnabled(false)
                .isNonLocked(false)
                .registeredAt(TIMESTAMP_PAST)
                .updatedAt(TIMESTAMP_PAST)
                .build();

        List<User> allUsers = List.of(user1, user2);
        Page<User> userPage = new PageImpl<>(allUsers, pageRequest, allUsers.size());
        long expectedTotalPages = (long) Math.ceil((double) allUsers.size() / SIZE);

        UserResponse userResponseDetailed1 = UserResponse.builder()
                .userId(user1.getUserId())
                .email(user1.getEmail())
                .firstName(user1.getFirstName())
                .lastName(user1.getLastName())
                .userRole(user1.getUserRole())
                .isEnabled(user1.getIsEnabled())
                .isNonLocked(user1.getIsNonLocked())
                .registeredAt(user1.getRegisteredAt())
                .updatedAt(user1.getUpdatedAt())
                .build();

        UserResponse userResponseDetailed2 = UserResponse.builder()
                .userId(user2.getUserId())
                .email(user2.getEmail())
                .firstName(user2.getFirstName())
                .lastName(user2.getLastName())
                .userRole(user2.getUserRole())
                .isEnabled(user2.getIsEnabled())
                .isNonLocked(user2.getIsNonLocked())
                .registeredAt(user2.getRegisteredAt())
                .updatedAt(user2.getUpdatedAt())
                .build();

        when(userRepository.findAllByIsEnabledAndIsNonLocked(isEnabled, isNonLocked, pageRequest)).thenReturn(userPage);
        when(userMapper.userToResponseDetailed(user1)).thenReturn(userResponseDetailed1);
        when(userMapper.userToResponseDetailed(user2)).thenReturn(userResponseDetailed2);

        Page<UserResponse> actualResponse = userService.getUsersByStatus(isEnabled, isNonLocked, SIZE, PAGE, ORDER, SORT_BY);

        verify(userRepository, times(1)).findAllByIsEnabledAndIsNonLocked(isEnabled, isNonLocked, pageRequest);
        verify(userMapper, times(1)).userToResponseDetailed(user1);
        verify(userMapper, times(1)).userToResponseDetailed(user2);

        assertNotNull(actualResponse);
        assertNotNull(actualResponse.getContent());
        assertEquals(allUsers.size(), actualResponse.getTotalElements());
        assertEquals(expectedTotalPages, actualResponse.getTotalPages());
        assertEquals((long)SIZE, actualResponse.getSize());
        assertEquals((long)PAGE, actualResponse.getNumber());

        assertNotNull(actualResponse.getContent());
        assertEquals(allUsers.size(), actualResponse.getContent().size());
        assertEquals(userResponseDetailed1.getUserId(), actualResponse.getContent().getFirst().getUserId());
        assertEquals(userResponseDetailed1.getEmail(), actualResponse.getContent().getFirst().getEmail());
        assertEquals(userResponseDetailed1.getFirstName(), actualResponse.getContent().getFirst().getFirstName());
        assertEquals(userResponseDetailed1.getLastName(), actualResponse.getContent().getFirst().getLastName());
        assertEquals(userResponseDetailed1.getUserRole(), actualResponse.getContent().getFirst().getUserRole());
        assertEquals(isEnabled, actualResponse.getContent().getFirst().getIsEnabled());
        assertEquals(isNonLocked, actualResponse.getContent().getFirst().getIsNonLocked());

        assertEquals(userResponseDetailed2.getUserId(), actualResponse.getContent().get(1).getUserId());
        assertEquals(userResponseDetailed2.getEmail(), actualResponse.getContent().get(1).getEmail());
        assertEquals(userResponseDetailed2.getFirstName(), actualResponse.getContent().get(1).getFirstName());
        assertEquals(userResponseDetailed2.getLastName(), actualResponse.getContent().get(1).getLastName());
        assertEquals(userResponseDetailed2.getUserRole(), actualResponse.getContent().get(1).getUserRole());
        assertEquals(isEnabled, actualResponse.getContent().get(1).getIsEnabled());
        assertEquals(isNonLocked, actualResponse.getContent().get(1).getIsNonLocked());
    }


    @Test
    void gerUsersByStatus_shouldReturnEmptyPagedModelWhenNoUsersMatch(){

        Boolean isEnabled = false;
        Boolean isNonLocked = false;

        Pageable pageRequest = PageRequest.of(PAGE, SIZE, Sort.Direction.fromString(ORDER), SORT_BY);

        Page<User> emptyPage = new PageImpl<>(Collections.emptyList(), pageRequest, 0);

        when(userRepository.findAllByIsEnabledAndIsNonLocked(isEnabled, isNonLocked, pageRequest)).thenReturn(emptyPage);

        Page<UserResponse> actualResponse = userService.getUsersByStatus(isEnabled, isNonLocked, SIZE, PAGE, ORDER, SORT_BY);

        verify(userRepository, times(1)).findAllByIsEnabledAndIsNonLocked(isEnabled, isNonLocked, pageRequest);
        verify(userMapper, never()).userToResponseDetailed(any(User.class));

        assertNotNull(actualResponse);
        assertNotNull(actualResponse.getContent());
        assertEquals(0L, actualResponse.getTotalElements());
        assertEquals(0L, actualResponse.getTotalPages());
        assertEquals((long)SIZE, actualResponse.getSize());
        assertEquals((long)PAGE, actualResponse.getNumber());

        assertNotNull(actualResponse.getContent());
        assertTrue(actualResponse.getContent().isEmpty());
        assertEquals(0, actualResponse.getContent().size());
    }

    @Test
    void getUserById_shouldReturnUserResponseWhenUserExists() {

        User existingUser = User.builder()
                .userId(USER_ID)
                .email("Email")
                .passwordHash(PASSWORD_HASH)
                .firstName("First Name")
                .lastName("Last Name")
                .userRole(USER_ROLE_CLIENT)
                .isEnabled(true)
                .isNonLocked(true)
                .registeredAt(TIMESTAMP_PAST)
                .updatedAt(TIMESTAMP_PAST)
                .build();

        UserResponse userResponse = UserResponse.builder()
                .userId(existingUser.getUserId())
                .email(existingUser.getEmail())
                .firstName(existingUser.getFirstName())
                .lastName(existingUser.getLastName())
                .userRole(existingUser.getUserRole())
                .registeredAt(existingUser.getRegisteredAt())
                .updatedAt(existingUser.getUpdatedAt())
                .build();

        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(existingUser));
        when(userMapper.userToResponse(existingUser)).thenReturn(userResponse);

        UserResponse actualResponse = userService.getUserById(USER_ID.toString());

        verify(userRepository, times(1)).findById(USER_ID);
        verify(userMapper, times(1)).userToResponse(existingUser);

        assertEquals(userResponse.getUserId(), actualResponse.getUserId());
        assertEquals(userResponse.getEmail(), actualResponse.getEmail());
        assertEquals(userResponse.getFirstName(), actualResponse.getFirstName());
        assertEquals(userResponse.getLastName(), actualResponse.getLastName());
        assertEquals(userResponse.getUserRole(), actualResponse.getUserRole());
    }

    @Test
    void getUserById_shouldThrowDataNotFoundExceptionWhenUserDoesNotExist() {

        when(userRepository.findById(NON_EXISTING_USER_ID)).thenReturn(Optional.empty());

        DataNotFoundException thrownException = assertThrows(DataNotFoundException.class, () ->
                userService.getUserById(NON_EXISTING_USER_ID.toString()));

        verify(userRepository, times(1)).findById(NON_EXISTING_USER_ID);
        verify(userMapper, never()).userToResponse(any(User.class));

        assertEquals(String.format("User with id: %s, was not found.", NON_EXISTING_USER_ID), thrownException.getMessage());
    }

    @Test
    void getUserById_shouldThrowIllegalArgumentExceptionWhenUserIdIsInvalidUuidString() {

        assertThrows(IllegalArgumentException.class, () -> userService.getUserById(INVALID_ID));

        verify(userRepository, never()).findById(any(UUID.class));
        verify(userMapper, never()).userToResponse(any(User.class));
    }


    @Test
    void updateUser_shouldUpdateUserSuccessfullyWhenUserExistsAndIsEnabledAndNonLocked() {

        UserUpdateRequest userUpdateRequest = UserUpdateRequest.builder()
                .firstName("Updated First Name")
                .lastName("Updated Last Name")
                .build();

        User existingUser = User.builder()
                .userId(USER_ID)
                .email("Original Email")
                .passwordHash(PASSWORD_HASH)
                .firstName("Original First Name")
                .lastName("Original Last Name")
                .userRole(USER_ROLE_CLIENT)
                .isEnabled(true)
                .isNonLocked(true)
                .registeredAt(TIMESTAMP_PAST)
                .updatedAt(TIMESTAMP_PAST)
                .build();

        User updatedUser = User.builder()
                .userId(existingUser.getUserId())
                .email(existingUser.getEmail())
                .passwordHash(existingUser.getPasswordHash())
                .firstName(userUpdateRequest.getFirstName())
                .lastName(userUpdateRequest.getLastName())
                .userRole(existingUser.getUserRole())
                .isEnabled(existingUser.getIsEnabled())
                .isNonLocked(existingUser.getIsNonLocked())
                .registeredAt(existingUser.getRegisteredAt())
                .updatedAt(TIMESTAMP_NOW)
                .build();

        UserResponse userResponse = UserResponse.builder()
                .userId(updatedUser.getUserId())
                .email(updatedUser.getEmail())
                .firstName(updatedUser.getFirstName())
                .lastName(updatedUser.getLastName())
                .userRole(updatedUser.getUserRole())
                .registeredAt(updatedUser.getRegisteredAt())
                .updatedAt(updatedUser.getUpdatedAt())
                .build();

        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);

        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(existingUser));
        when(userRepository.saveAndFlush(existingUser)).thenReturn(updatedUser);
        when(userMapper.userToResponse(updatedUser)).thenReturn(userResponse);

        UserResponse actualResponse = userService.updateUser(USER_ID.toString(), userUpdateRequest);

        verify(userRepository, times(1)).findById(USER_ID);

        verify(userRepository, times(1)).saveAndFlush(userCaptor.capture());
        User capturedUser = userCaptor.getValue();
        assertNotNull(capturedUser);
        assertEquals(existingUser.getUserId(), capturedUser.getUserId());
        assertEquals(existingUser.getEmail(), capturedUser.getEmail());
        assertEquals(existingUser.getPasswordHash(), capturedUser.getPasswordHash());
        assertEquals(userUpdateRequest.getFirstName(), capturedUser.getFirstName());
        assertEquals(userUpdateRequest.getLastName(), capturedUser.getLastName());
        assertEquals(existingUser.getUserRole(), capturedUser.getUserRole());
        assertEquals(existingUser.getIsEnabled(), capturedUser.getIsEnabled());
        assertEquals(existingUser.getIsNonLocked(), capturedUser.getIsNonLocked());
        assertEquals(existingUser.getRegisteredAt(), capturedUser.getRegisteredAt());

        verify(userMapper, times(1)).userToResponse(updatedUser);

        assertNotNull(actualResponse);
        assertEquals(userResponse.getUserId(), actualResponse.getUserId());
        assertEquals(userResponse.getEmail(), actualResponse.getEmail());
        assertEquals(userResponse.getFirstName(), actualResponse.getFirstName());
        assertEquals(userResponse.getLastName(), actualResponse.getLastName());
        assertEquals(userResponse.getUserRole(), actualResponse.getUserRole());
        assertEquals(userResponse.getRegisteredAt(), actualResponse.getRegisteredAt());
        assertTrue(actualResponse.getUpdatedAt().isAfter(existingUser.getUpdatedAt()));
    }

    @Test
    void updateUser_shouldUpdateOnlyProvidedFieldsAndReturnUpdatedUserResponse() {

        UserUpdateRequest userUpdateRequest = UserUpdateRequest.builder()
                .firstName("Updated First Name")
                .lastName(null)
                .build();

        User existingUser = User.builder()
                .userId(USER_ID)
                .email("Original Email")
                .passwordHash(PASSWORD_HASH)
                .firstName("Original First Name")
                .lastName("Original Last Name")
                .userRole(USER_ROLE_CLIENT)
                .isEnabled(true)
                .isNonLocked(true)
                .registeredAt(TIMESTAMP_PAST)
                .updatedAt(TIMESTAMP_PAST)
                .build();

        User updatedUser = User.builder()
                .userId(existingUser.getUserId())
                .email(existingUser.getEmail())
                .passwordHash(existingUser.getPasswordHash())
                .firstName(userUpdateRequest.getFirstName())
                .lastName(existingUser.getLastName())
                .userRole(existingUser.getUserRole())
                .isEnabled(existingUser.getIsEnabled())
                .isNonLocked(existingUser.getIsNonLocked())
                .registeredAt(existingUser.getRegisteredAt())
                .updatedAt(TIMESTAMP_NOW)
                .build();

        UserResponse userResponse = UserResponse.builder()
                .userId(updatedUser.getUserId())
                .email(updatedUser.getEmail())
                .firstName(updatedUser.getFirstName())
                .lastName(updatedUser.getLastName())
                .userRole(updatedUser.getUserRole())
                .registeredAt(updatedUser.getRegisteredAt())
                .updatedAt(updatedUser.getUpdatedAt())
                .build();

        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);

        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(existingUser));
        when(userRepository.saveAndFlush(existingUser)).thenReturn(updatedUser);
        when(userMapper.userToResponse(updatedUser)).thenReturn(userResponse);

        UserResponse actualResponse = userService.updateUser(USER_ID.toString(), userUpdateRequest);

        verify(userRepository, times(1)).findById(USER_ID);

        verify(userRepository, times(1)).saveAndFlush(userCaptor.capture());
        User capturedUser = userCaptor.getValue();
        assertNotNull(capturedUser);
        assertEquals(existingUser.getUserId(), capturedUser.getUserId());
        assertEquals(existingUser.getEmail(), capturedUser.getEmail());
        assertEquals(existingUser.getPasswordHash(), capturedUser.getPasswordHash());
        assertEquals(userUpdateRequest.getFirstName(), capturedUser.getFirstName());
        assertEquals(existingUser.getLastName(), capturedUser.getLastName());
        assertEquals(existingUser.getUserRole(), capturedUser.getUserRole());
        assertEquals(existingUser.getIsEnabled(), capturedUser.getIsEnabled());
        assertEquals(existingUser.getIsNonLocked(), capturedUser.getIsNonLocked());
        assertEquals(existingUser.getRegisteredAt(), capturedUser.getRegisteredAt());

        verify(userMapper, times(1)).userToResponse(updatedUser);

        assertNotNull(actualResponse);
        assertEquals(userResponse.getUserId(), actualResponse.getUserId());
        assertEquals(userResponse.getEmail(), actualResponse.getEmail());
        assertEquals(userResponse.getFirstName(), actualResponse.getFirstName());
        assertEquals(userResponse.getLastName(), actualResponse.getLastName());
        assertEquals(userResponse.getUserRole(), actualResponse.getUserRole());
        assertEquals(userResponse.getRegisteredAt(), actualResponse.getRegisteredAt());
        assertTrue(actualResponse.getUpdatedAt().isAfter(existingUser.getUpdatedAt()));
    }

    @Test
    void updateUser_shouldThrowDataNotFoundExceptionWhenUserDoesNotExist() {

        UserUpdateRequest userUpdateRequest = UserUpdateRequest.builder()
                .firstName("Updated First Name")
                .lastName("Updated Last Name")
                .build();

        when(userRepository.findById(NON_EXISTING_USER_ID)).thenReturn(Optional.empty());

        DataNotFoundException thrownException = assertThrows(DataNotFoundException.class, () -> userService.updateUser(NON_EXISTING_USER_ID.toString(), userUpdateRequest));

        verify(userRepository, times(1)).findById(NON_EXISTING_USER_ID);
        verify(userRepository, never()).saveAndFlush(any(User.class));
        verify(userMapper, never()).userToResponse(any(User.class));

        assertEquals(String.format("User with id: %s, was not found.", NON_EXISTING_USER_ID), thrownException.getMessage());
    }

    @Test
    void updateUser_shouldThrowIllegalArgumentExceptionWhenUserIdIsInvalidUuidString() {

        UserUpdateRequest userUpdateRequest = UserUpdateRequest.builder()
                .firstName("Updated First Name")
                .lastName("Updated Last Name")
                .build();

        assertThrows(IllegalArgumentException.class, () -> userService.updateUser(INVALID_ID, userUpdateRequest));

        verify(userRepository, never()).findById(any(UUID.class));
        verify(userRepository, never()).saveAndFlush(any(User.class));
        verify(userMapper, never()).userToResponse(any(User.class));
    }

    @Test
    void updateUser_shouldThrowIllegalArgumentExceptionWhenUserIsDisabled() {

        UserUpdateRequest userUpdateRequest = UserUpdateRequest.builder()
                .firstName("Updated First Name")
                .lastName("Updated Last Name")
                .build();

        User existingUser = User.builder()
                .userId(USER_ID)
                .email("Original Email")
                .passwordHash(PASSWORD_HASH)
                .firstName("Original First Name")
                .lastName("Original Last Name")
                .userRole(USER_ROLE_CLIENT)
                .isEnabled(false)
                .isNonLocked(true)
                .registeredAt(TIMESTAMP_PAST)
                .updatedAt(TIMESTAMP_PAST)
                .build();

        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(existingUser));

        IllegalArgumentException thrownException = assertThrows(IllegalArgumentException.class, () -> userService.updateUser(USER_ID.toString(), userUpdateRequest));

        verify(userRepository, times(1)).findById(USER_ID);
        verify(userRepository, never()).saveAndFlush(any(User.class));
        verify(userMapper, never()).userToResponse(any(User.class));

        assertEquals(String.format("User with id: %s, is unregistered and can not be updated.", USER_ID), thrownException.getMessage());
    }

    @Test
    void updateUser_shouldThrowIllegalArgumentExceptionWhenUserIsLocked() {

        UserUpdateRequest userUpdateRequest = UserUpdateRequest.builder()
                .firstName("Updated First Name")
                .lastName("Updated Last Name")
                .build();

        User existingUser = User.builder()
                .userId(USER_ID)
                .email("Original Email")
                .passwordHash(PASSWORD_HASH)
                .firstName("Original First Name")
                .lastName("Original Last Name")
                .userRole(USER_ROLE_CLIENT)
                .isEnabled(true)
                .isNonLocked(false)
                .registeredAt(TIMESTAMP_PAST)
                .updatedAt(TIMESTAMP_PAST)
                .build();

        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(existingUser));

        IllegalArgumentException thrownException = assertThrows(IllegalArgumentException.class, () -> userService.updateUser(USER_ID.toString(), userUpdateRequest));

        verify(userRepository, times(1)).findById(USER_ID);
        verify(userRepository, never()).saveAndFlush(any(User.class));
        verify(userMapper, never()).userToResponse(any(User.class));

        assertEquals(String.format("User with id: %s, is locked and can not be updated.", USER_ID), thrownException.getMessage());
    }

    @Test
    void setUserRole_shouldSetUserRoleSuccessfullyWhenUserExistsAndIsEnabledAndNonLocked() {

        User existingUser = User.builder()
                .userId(USER_ID)
                .email("Email")
                .passwordHash(PASSWORD_HASH)
                .firstName("First Name")
                .lastName("Last Name")
                .userRole(USER_ROLE_CLIENT)
                .isEnabled(true)
                .isNonLocked(true)
                .registeredAt(TIMESTAMP_PAST)
                .updatedAt(TIMESTAMP_PAST)
                .build();

        User updatedUser = User.builder()
                .userId(existingUser.getUserId())
                .email(existingUser.getEmail())
                .passwordHash(existingUser.getPasswordHash())
                .firstName(existingUser.getFirstName())
                .lastName(existingUser.getLastName())
                .userRole(USER_ROLE_ADMIN)
                .isEnabled(existingUser.getIsEnabled())
                .isNonLocked(existingUser.getIsNonLocked())
                .registeredAt(existingUser.getRegisteredAt())
                .updatedAt(TIMESTAMP_NOW)
                .build();

        MessageResponse messageResponse = MessageResponse.builder()
                .message(String.format("UserRole %s was set for user with id: %s.", USER_ROLE_ADMIN.name(), USER_ID))
                .build();

        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);

        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(existingUser));
        when(userRepository.saveAndFlush(existingUser)).thenReturn(updatedUser);

        MessageResponse actualResponse = userService.setUserRole(USER_ID.toString(), USER_ROLE_ADMIN.name());

        verify(userRepository, times(1)).findById(USER_ID);
        verify(userRepository, times(1)).saveAndFlush(userCaptor.capture());
        User capturedUser = userCaptor.getValue();
        assertNotNull(capturedUser);
        assertEquals(existingUser.getUserId(), capturedUser.getUserId());
        assertEquals(existingUser.getEmail(), capturedUser.getEmail());
        assertEquals(existingUser.getPasswordHash(), capturedUser.getPasswordHash());
        assertEquals(existingUser.getFirstName(), capturedUser.getFirstName());
        assertEquals(existingUser.getLastName(), capturedUser.getLastName());
        assertEquals(USER_ROLE_ADMIN, capturedUser.getUserRole());
        assertEquals(existingUser.getIsEnabled(), capturedUser.getIsEnabled());
        assertEquals(existingUser.getIsNonLocked(), capturedUser.getIsNonLocked());
        assertEquals(existingUser.getRegisteredAt(), capturedUser.getRegisteredAt());
        assertTrue(updatedUser.getUpdatedAt().isAfter(existingUser.getUpdatedAt()));

        assertNotNull(actualResponse);
        assertEquals(messageResponse.getMessage(), actualResponse.getMessage());
    }

    @Test
    void setUserRole_shouldThrowIllegalArgumentExceptionWhenUserIdIsInvalidUuidString() {

        assertThrows(IllegalArgumentException.class, () -> userService.setUserRole(INVALID_ID, USER_ROLE_ADMIN.name()));

        verify(userRepository, never()).findById(any(UUID.class));
        verify(userRepository, never()).saveAndFlush(any(User.class));
        verify(userMapper, never()).userToResponse(any(User.class));
    }

    @Test
    void setUserRole_shouldThrowDataNotFoundExceptionWhenUserDoesNotExist() {

        when(userRepository.findById(NON_EXISTING_USER_ID)).thenReturn(Optional.empty());

        DataNotFoundException thrownException = assertThrows(DataNotFoundException.class, () -> userService.setUserRole(NON_EXISTING_USER_ID.toString(), USER_ROLE_ADMIN.name()));

        verify(userRepository, times(1)).findById(NON_EXISTING_USER_ID);
        verify(userRepository, never()).saveAndFlush(any(User.class));

        assertEquals(String.format("User with id: %s, was not found.", NON_EXISTING_USER_ID), thrownException.getMessage());
    }

    @Test
    void setUserRole_shouldThrowIllegalArgumentExceptionWhenUserIsLocked() {

        User existingUser = User.builder()
                .userId(USER_ID)
                .email("Email")
                .passwordHash(PASSWORD_HASH)
                .firstName("First Name")
                .lastName("Last Name")
                .userRole(USER_ROLE_CLIENT)
                .isEnabled(true)
                .isNonLocked(false)
                .registeredAt(TIMESTAMP_PAST)
                .updatedAt(TIMESTAMP_PAST)
                .build();

        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(existingUser));

        IllegalArgumentException thrownException = assertThrows(IllegalArgumentException.class, () -> userService.setUserRole(USER_ID.toString(), USER_ROLE_ADMIN.name()));

        verify(userRepository, times(1)).findById(USER_ID);
        verify(userRepository, never()).saveAndFlush(any(User.class));

        assertEquals(String.format("User with id: %s, is locked and his userRole can not be changed.", USER_ID), thrownException.getMessage());
    }

    @Test
    void setUserRole_shouldThrowIllegalArgumentExceptionWhenUserIsDisabled() {

        User existingUser = User.builder()
                .userId(USER_ID)
                .email("Email")
                .passwordHash(PASSWORD_HASH)
                .firstName("First Name")
                .lastName("Last Name")
                .userRole(USER_ROLE_CLIENT)
                .isEnabled(false)
                .isNonLocked(true)
                .registeredAt(TIMESTAMP_PAST)
                .updatedAt(TIMESTAMP_PAST)
                .build();

        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(existingUser));

        IllegalArgumentException thrownException = assertThrows(IllegalArgumentException.class, () -> userService.setUserRole(USER_ID.toString(), USER_ROLE_ADMIN.name()));

        verify(userRepository, times(1)).findById(USER_ID);
        verify(userRepository, never()).saveAndFlush(any(User.class));

        assertEquals(String.format("User with id: %s, is unregistered and his userRole can not be changed.", USER_ID), thrownException.getMessage());
    }

    @Test
    void setUserRole_shouldThrowIllegalArgumentExceptionWhenUserHasAlreadyHasTargetRole() {

        String sameRole = UserRole.CLIENT.toString();

        User existingUser = User.builder()
                .userId(USER_ID)
                .email("Email")
                .passwordHash(PASSWORD_HASH)
                .firstName("First Name")
                .lastName("Last Name")
                .userRole(USER_ROLE_CLIENT)
                .isEnabled(true)
                .isNonLocked(true)
                .registeredAt(TIMESTAMP_PAST)
                .updatedAt(TIMESTAMP_PAST)
                .build();

        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(existingUser));

        IllegalArgumentException thrownException = assertThrows(IllegalArgumentException.class, () -> userService.setUserRole(USER_ID.toString(), sameRole));

        verify(userRepository, times(1)).findById(USER_ID);
        verify(userRepository, never()).saveAndFlush(any(User.class));

        assertEquals(String.format("User with id: %s, already has userRole '%s'.", USER_ID, sameRole), thrownException.getMessage());
    }

    @Test
    void toggleLockState_shouldLockUserWhenUserIsEnabledAndNonLocked() {

        User existingUser = User.builder()
                .userId(USER_ID)
                .email("Email")
                .passwordHash(PASSWORD_HASH)
                .firstName("First Name")
                .lastName("Last Name")
                .userRole(USER_ROLE_CLIENT)
                .isEnabled(true)
                .isNonLocked(true)
                .registeredAt(TIMESTAMP_PAST)
                .updatedAt(TIMESTAMP_PAST)
                .build();

        User updatedUser = User.builder()
                .userId(existingUser.getUserId())
                .email(existingUser.getEmail())
                .passwordHash(existingUser.getPasswordHash())
                .firstName(existingUser.getFirstName())
                .lastName(existingUser.getLastName())
                .userRole(existingUser.getUserRole())
                .isEnabled(existingUser.getIsEnabled())
                .isNonLocked(!existingUser.getIsNonLocked())
                .registeredAt(existingUser.getRegisteredAt())
                .updatedAt(TIMESTAMP_NOW)
                .build();

        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);

        MessageResponse messageResponse = MessageResponse.builder()
                .message(String.format("User with id: %s has been %s.", USER_ID, existingUser.getIsNonLocked() ? "locked" : "unlocked"))
                .build();

        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(existingUser));
        when(userRepository.saveAndFlush(existingUser)).thenReturn(updatedUser);

        MessageResponse actualResponse = userService.toggleLockState(USER_ID.toString());

        verify(userRepository, times(1)).findById(USER_ID);
        verify(userRepository, times(1)).saveAndFlush(userCaptor.capture());
        User capturedUser = userCaptor.getValue();
        assertNotNull(capturedUser);
        assertEquals(existingUser.getUserId(), capturedUser.getUserId());
        assertEquals(existingUser.getEmail(), capturedUser.getEmail());
        assertEquals(existingUser.getPasswordHash(), capturedUser.getPasswordHash());
        assertEquals(existingUser.getFirstName(), capturedUser.getFirstName());
        assertEquals(existingUser.getLastName(), capturedUser.getLastName());
        assertEquals(existingUser.getUserRole(), capturedUser.getUserRole());
        assertEquals(existingUser.getIsEnabled(), capturedUser.getIsEnabled());
        assertFalse(capturedUser.getIsNonLocked());
        assertEquals(existingUser.getRegisteredAt(), capturedUser.getRegisteredAt());
        assertTrue(updatedUser.getUpdatedAt().isAfter(existingUser.getUpdatedAt()));

        assertNotNull(actualResponse);
        assertEquals(messageResponse.getMessage(), actualResponse.getMessage());
    }

    @Test
    void toggleLockState_shouldUnlockUserWhenUserIsEnabledAndLocked() {

        User existingUser = User.builder()
                .userId(USER_ID)
                .email("Email")
                .passwordHash(PASSWORD_HASH)
                .firstName("First Name")
                .lastName("Last Name")
                .userRole(USER_ROLE_CLIENT)
                .isEnabled(true)
                .isNonLocked(false)
                .registeredAt(TIMESTAMP_PAST)
                .updatedAt(TIMESTAMP_PAST)
                .build();

        User updatedUser = User.builder()
                .userId(existingUser.getUserId())
                .email(existingUser.getEmail())
                .passwordHash(existingUser.getPasswordHash())
                .firstName(existingUser.getFirstName())
                .lastName(existingUser.getLastName())
                .userRole(existingUser.getUserRole())
                .isEnabled(existingUser.getIsEnabled())
                .isNonLocked(!existingUser.getIsNonLocked())
                .registeredAt(existingUser.getRegisteredAt())
                .updatedAt(TIMESTAMP_NOW)
                .build();

        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);

        MessageResponse messageResponse = MessageResponse.builder()
                .message(String.format("User with id: %s has been %s.", USER_ID, existingUser.getIsNonLocked() ? "locked" : "unlocked"))
                .build();

        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(existingUser));
        when(userRepository.saveAndFlush(existingUser)).thenReturn(updatedUser);

        MessageResponse actualResponse = userService.toggleLockState(USER_ID.toString());

        verify(userRepository, times(1)).findById(USER_ID);
        verify(userRepository, times(1)).saveAndFlush(userCaptor.capture());
        User capturedUser = userCaptor.getValue();
        assertNotNull(capturedUser);
        assertEquals(existingUser.getUserId(), capturedUser.getUserId());
        assertEquals(existingUser.getEmail(), capturedUser.getEmail());
        assertEquals(existingUser.getPasswordHash(), capturedUser.getPasswordHash());
        assertEquals(existingUser.getFirstName(), capturedUser.getFirstName());
        assertEquals(existingUser.getLastName(), capturedUser.getLastName());
        assertEquals(existingUser.getUserRole(), capturedUser.getUserRole());
        assertEquals(existingUser.getIsEnabled(), capturedUser.getIsEnabled());
        assertTrue(capturedUser.getIsNonLocked());
        assertEquals(existingUser.getRegisteredAt(), capturedUser.getRegisteredAt());
        assertTrue(updatedUser.getUpdatedAt().isAfter(existingUser.getUpdatedAt()));

        assertNotNull(actualResponse);
        assertEquals(messageResponse.getMessage(), actualResponse.getMessage());
    }

    @Test
    void toggleLockState_shouldThrowIllegalArgumentExceptionWhenUserIdIsInvalidUuidString() {

        assertThrows(IllegalArgumentException.class, () -> userService.toggleLockState(INVALID_ID));

        verify(userRepository, never()).findById(any(UUID.class));
        verify(userMapper, never()).userToResponse(any(User.class));
    }

    @Test
    void toggleLockState_shouldThrowDataNotFoundExceptionWhenUserDoesNotExist() {

        when(userRepository.findById(NON_EXISTING_USER_ID)).thenReturn(Optional.empty());

        DataNotFoundException thrown = assertThrows(DataNotFoundException.class, () ->
                userService.toggleLockState(NON_EXISTING_USER_ID.toString()));

        verify(userRepository, times(1)).findById(NON_EXISTING_USER_ID);
        verify(userRepository, never()).saveAndFlush(any(User.class));

        assertEquals(String.format("User with id: %s, was not found.", NON_EXISTING_USER_ID), thrown.getMessage());
    }

    @Test
    void toggleLockState_shouldThrowIllegalArgumentExceptionWhenUserIsDisabled() {

        User existingUser = User.builder()
                .userId(USER_ID)
                .email("Email")
                .passwordHash(PASSWORD_HASH)
                .firstName("First Name")
                .lastName("Last Name")
                .userRole(USER_ROLE_CLIENT)
                .isEnabled(false)
                .isNonLocked(true)
                .registeredAt(TIMESTAMP_PAST)
                .updatedAt(TIMESTAMP_PAST)
                .build();

        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(existingUser));

        IllegalArgumentException thrown = assertThrows(IllegalArgumentException.class, () -> userService.toggleLockState(USER_ID.toString()));

        verify(userRepository, times(1)).findById(USER_ID);
        verify(userRepository, never()).saveAndFlush(any(User.class));

        assertEquals(String.format("User with id: %s, is unregistered and can not be locked or unlocked.", USER_ID), thrown.getMessage());
    }

    @Test
    void unregisterUser_shouldSucceedWhenUserIsValidAndNotLockedAndPasswordsMatch() {

        UserUnregisterRequest request = UserUnregisterRequest.builder()
                .password(PASSWORD)
                .confirmPassword(PASSWORD)
                .build();

        User existingUser = User.builder()
                .userId(USER_ID)
                .email("Email")
                .passwordHash(PASSWORD_HASH)
                .firstName("First Name")
                .lastName("Last Name")
                .userRole(USER_ROLE_CLIENT)
                .isEnabled(true)
                .isNonLocked(true)
                .registeredAt(TIMESTAMP_PAST)
                .updatedAt(TIMESTAMP_PAST)
                .build();

        User unregisteredUser = User.builder()
                .userId(existingUser.getUserId())
                .email(String.format("%s@example.com", existingUser.getUserId()))
                .passwordHash(existingUser.getPasswordHash())
                .firstName("Deleted User")
                .lastName("Deleted User")
                .userRole(existingUser.getUserRole())
                .isEnabled(false)
                .isNonLocked(existingUser.getIsNonLocked())
                .registeredAt(existingUser.getRegisteredAt())
                .updatedAt(TIMESTAMP_NOW)
                .build();

        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);

        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(existingUser));
        when(passwordEncoder.matches(PASSWORD, PASSWORD_HASH)).thenReturn(true);
        when(userRepository.saveAndFlush(existingUser)).thenReturn(unregisteredUser);

        MessageResponse messageResponse = userService.unregisterUser(USER_ID.toString(), request);

        verify(userRepository, times(1)).findById(USER_ID);
        verify(passwordEncoder, times(1)).matches(PASSWORD, PASSWORD_HASH);
        verify(userRepository, times(1)).saveAndFlush(userCaptor.capture());

        User capturedUser = userCaptor.getValue();
        assertEquals(String.format("%s@example.com", USER_ID), capturedUser.getEmail());
        assertEquals("Deleted User", capturedUser.getFirstName());
        assertEquals("Deleted User", capturedUser.getLastName());
        assertNull(capturedUser.getRefreshToken());
        assertFalse(capturedUser.getIsEnabled());

        assertNotNull(messageResponse);
        assertEquals(String.format("User with id: %s, has been unregistered.", USER_ID), messageResponse.getMessage());
    }

    @Test
    void unregisterUser_shouldThrowBadCredentialsExceptionWhenPasswordsMismatch() {

        UserUnregisterRequest request = UserUnregisterRequest.builder()
                .password(PASSWORD)
                .confirmPassword("wrongPassword")
                .build();

        BadCredentialsException thrown = assertThrows(BadCredentialsException.class, () -> userService.unregisterUser(USER_ID.toString(), request));

        verify(userRepository, never()).findById(any(UUID.class));
        verify(passwordEncoder, never()).matches(anyString(), anyString());
        verify(userRepository, never()).saveAndFlush(any(User.class));

        assertEquals("Password doesn't match the CONFIRM PASSWORD field.", thrown.getMessage());
    }

    @Test
    void unregisterUser_shouldThrowIllegalArgumentExceptionWhenUserIdIsInvalidUuidString() {

        UserUnregisterRequest request = UserUnregisterRequest.builder()
                .password(PASSWORD)
                .confirmPassword(PASSWORD)
                .build();

        assertThrows(IllegalArgumentException.class, () -> userService.unregisterUser(INVALID_ID, request));

        verify(userRepository, never()).findById(any(UUID.class));
        verify(passwordEncoder, never()).matches(anyString(), anyString());
        verify(userRepository, never()).saveAndFlush(any(User.class));
    }

    @Test
    void unregisterUser_shouldThrowDataNotFoundExceptionWhenUserNotFound() {

        UserUnregisterRequest request = UserUnregisterRequest.builder()
                .password(PASSWORD)
                .confirmPassword(PASSWORD)
                .build();

        when(userRepository.findById(NON_EXISTING_USER_ID)).thenReturn(Optional.empty());

        DataNotFoundException thrown = assertThrows(DataNotFoundException.class, () ->
                userService.unregisterUser(NON_EXISTING_USER_ID.toString(), request));

        verify(userRepository, times(1)).findById(NON_EXISTING_USER_ID);
        verify(passwordEncoder, never()).matches(anyString(), anyString());
        verify(userRepository, never()).saveAndFlush(any(User.class));

        assertEquals(String.format("User with id: %s, was not found.", NON_EXISTING_USER_ID), thrown.getMessage());
    }

    @Test
    void unregisterUser_shouldThrowBadCredentialsExceptionWhenPasswordIncorrect() {

        String invalidPassword = "Invalid Password";

        UserUnregisterRequest request = UserUnregisterRequest.builder()
                .password(invalidPassword)
                .confirmPassword(invalidPassword)
                .build();

        User existingUser = User.builder()
                .userId(USER_ID)
                .email("Email")
                .passwordHash(PASSWORD_HASH)
                .firstName("First Name")
                .lastName("Last Name")
                .userRole(USER_ROLE_CLIENT)
                .isEnabled(true)
                .isNonLocked(true)
                .registeredAt(TIMESTAMP_PAST)
                .updatedAt(TIMESTAMP_PAST)
                .build();

        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(existingUser));
        when(passwordEncoder.matches(invalidPassword, PASSWORD_HASH)).thenReturn(false);

        BadCredentialsException thrown = assertThrows(BadCredentialsException.class, () ->
                userService.unregisterUser(USER_ID.toString(), request)
        );

        verify(userRepository, times(1)).findById(USER_ID);
        verify(passwordEncoder, times(1)).matches(invalidPassword, PASSWORD_HASH);
        verify(userRepository, never()).saveAndFlush(any(User.class));

        assertEquals("Given password doesn't match the password saved in database.", thrown.getMessage());
    }

    @Test
    void unregisterUser_shouldThrowIllegalArgumentExceptionWhenUserIsLocked() {

        UserUnregisterRequest request = UserUnregisterRequest.builder()
                .password(PASSWORD)
                .confirmPassword(PASSWORD)
                .build();

        User lockedUser = User.builder()
                .userId(USER_ID)
                .email("Email")
                .passwordHash(PASSWORD_HASH)
                .firstName("First Name")
                .lastName("Last Name")
                .userRole(USER_ROLE_CLIENT)
                .isEnabled(true)
                .isNonLocked(false)
                .registeredAt(TIMESTAMP_PAST)
                .updatedAt(TIMESTAMP_PAST)
                .build();

        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(lockedUser));
        when(passwordEncoder.matches(PASSWORD, PASSWORD_HASH)).thenReturn(true);

        IllegalArgumentException thrown = assertThrows(IllegalArgumentException.class, () ->
                userService.unregisterUser(USER_ID.toString(), request));

        verify(userRepository, times(1)).findById(USER_ID);
        verify(passwordEncoder, times(1)).matches(PASSWORD, PASSWORD_HASH);
        verify(userRepository, never()).saveAndFlush(any(User.class));

        assertEquals(String.format("User with id: %s, is locked and can not be unregistered.", USER_ID), thrown.getMessage());
    }

    @Test
    void unregisterUser_shouldThrowIllegalArgumentExceptionWhenUserIsAlreadyUnregistered() {

        UserUnregisterRequest request = UserUnregisterRequest.builder()
                .password(PASSWORD)
                .confirmPassword(PASSWORD)
                .build();

        User unregisteredUser = User.builder()
                .userId(USER_ID)
                .email("Email")
                .passwordHash(PASSWORD_HASH)
                .firstName("First Name")
                .lastName("Last Name")
                .userRole(USER_ROLE_CLIENT)
                .isEnabled(false)
                .isNonLocked(true)
                .registeredAt(TIMESTAMP_PAST)
                .updatedAt(TIMESTAMP_PAST)
                .build();

        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(unregisteredUser));
        when(passwordEncoder.matches(PASSWORD, PASSWORD_HASH)).thenReturn(true);

        IllegalArgumentException thrown = assertThrows(IllegalArgumentException.class, () ->
                userService.unregisterUser(USER_ID.toString(), request));

        verify(userRepository, times(1)).findById(USER_ID);
        verify(passwordEncoder, times(1)).matches(PASSWORD, PASSWORD_HASH);
        verify(userRepository, never()).saveAndFlush(any(User.class));

        assertEquals(String.format("User with id: %s, is already unregistered.", USER_ID), thrown.getMessage());
    }
}