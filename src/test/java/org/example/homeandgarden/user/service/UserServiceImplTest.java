package org.example.homeandgarden.user.service;

import org.example.homeandgarden.exception.DataNotFoundException;
import org.example.homeandgarden.exception.UserDisabledException;
import org.example.homeandgarden.exception.UserLockedException;
import org.example.homeandgarden.shared.MessageResponse;
import org.example.homeandgarden.user.dto.ChangePasswordRequest;
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
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
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

    private static final Integer PAGE = 0;
    private static final Integer SIZE = 5;
    private static final String ORDER = "ASC";
    private static final String SORT_BY = "createdAt";

    private static final UUID USER_1_ID = UUID.fromString("f81d4fae-7dec-11d0-a765-00a0c91e6bf6");
    private static final UUID USER_2_ID = UUID.fromString("e9b1d3b0-146a-4be0-a1e2-2b9e18b1a8cf");

    private static final UUID USER_ID = UUID.fromString("aebdd1a2-1bc2-4cc9-8496-674e4c7ee5f2");
    private static final UUID NON_EXISTING_USER_ID = UUID.fromString("de305d54-75b4-431b-adb2-eb6b9e546014");

    private static final String INVALID_ID = "Invalid UUID";

    private static final String USER_EMAIL = "user@example.com";
    private static final String NON_EXISTING_USER_EMAIL = "nonExistingUser@example.com";

    private static final UserRole USER_ROLE_CLIENT = UserRole.CLIENT;
    private static final UserRole USER_ROLE_ADMIN = UserRole.ADMINISTRATOR;

    private static final String PASSWORD = "Raw Password";
    private static final String PASSWORD_HASH = "Hashed Password";
    private static final String NEW_PASSWORD = "New Password";
    private static final String NEW_PASSWORD_HASH = "New Hashed Password";

    private final Instant TIMESTAMP_NOW = Instant.now();
    private static final Instant TIMESTAMP_PAST = Instant.parse("2024-12-01T12:00:00Z");

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
    void getMyProfile_shouldReturnUserResponseWhenUserExists() {

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

        when(userRepository.findByEmail(USER_EMAIL)).thenReturn(Optional.of(existingUser));
        when(userMapper.userToResponse(existingUser)).thenReturn(userResponse);

        UserResponse actualResponse = userService.getMyProfile(USER_EMAIL);

        verify(userRepository, times(1)).findByEmail(USER_EMAIL);
        verify(userMapper, times(1)).userToResponse(existingUser);

        assertEquals(userResponse.getUserId(), actualResponse.getUserId());
        assertEquals(userResponse.getEmail(), actualResponse.getEmail());
        assertEquals(userResponse.getFirstName(), actualResponse.getFirstName());
        assertEquals(userResponse.getLastName(), actualResponse.getLastName());
        assertEquals(userResponse.getUserRole(), actualResponse.getUserRole());
    }

    @Test
    void getMyProfile_shouldThrowDataNotFoundExceptionWhenUserDoesNotExist() {

        when(userRepository.findByEmail(NON_EXISTING_USER_EMAIL)).thenReturn(Optional.empty());

        DataNotFoundException thrownException = assertThrows(DataNotFoundException.class, () ->
                userService.getMyProfile(NON_EXISTING_USER_EMAIL));

        verify(userRepository, times(1)).findByEmail(NON_EXISTING_USER_EMAIL);
        verify(userMapper, never()).userToResponse(any(User.class));

        assertEquals(String.format("User with email: %s, was not found.", NON_EXISTING_USER_EMAIL), thrownException.getMessage());
    }

    @Test
    void updateMyProfile_shouldUpdateUserSuccessfullyWhenUserExists() {

        UserUpdateRequest userUpdateRequest = UserUpdateRequest.builder()
                .firstName("Updated First Name")
                .lastName("Updated Last Name")
                .build();

        User existingUser = User.builder()
                .userId(USER_ID)
                .email(USER_EMAIL)
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

        when(userRepository.findByEmail(USER_EMAIL)).thenReturn(Optional.of(existingUser));
        when(userRepository.saveAndFlush(existingUser)).thenReturn(updatedUser);
        when(userMapper.userToResponse(updatedUser)).thenReturn(userResponse);

        UserResponse actualResponse = userService.updateMyProfile(USER_EMAIL, userUpdateRequest);

        verify(userRepository, times(1)).findByEmail(USER_EMAIL);

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
    void updateMyProfile_shouldUpdateOnlyProvidedFieldsAndReturnUpdatedUserResponse() {

        UserUpdateRequest userUpdateRequest = UserUpdateRequest.builder()
                .firstName("Updated First Name")
                .lastName(null)
                .build();

        User existingUser = User.builder()
                .userId(USER_ID)
                .email(USER_EMAIL)
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

        when(userRepository.findByEmail(USER_EMAIL)).thenReturn(Optional.of(existingUser));
        when(userRepository.saveAndFlush(existingUser)).thenReturn(updatedUser);
        when(userMapper.userToResponse(updatedUser)).thenReturn(userResponse);

        UserResponse actualResponse = userService.updateMyProfile(USER_EMAIL, userUpdateRequest);

        verify(userRepository, times(1)).findByEmail(USER_EMAIL);

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
    void updateMyProfile_shouldThrowDataNotFoundExceptionWhenUserDoesNotExist() {

        UserUpdateRequest userUpdateRequest = UserUpdateRequest.builder()
                .firstName("Updated First Name")
                .lastName("Updated Last Name")
                .build();

        when(userRepository.findByEmail(NON_EXISTING_USER_EMAIL)).thenReturn(Optional.empty());

        DataNotFoundException thrownException = assertThrows(DataNotFoundException.class, () -> userService.updateMyProfile(NON_EXISTING_USER_EMAIL, userUpdateRequest));

        verify(userRepository, times(1)).findByEmail(NON_EXISTING_USER_EMAIL);
        verify(userRepository, never()).saveAndFlush(any(User.class));
        verify(userMapper, never()).userToResponse(any(User.class));

        assertEquals(String.format("User with email: %s, was not found.", NON_EXISTING_USER_EMAIL), thrownException.getMessage());
    }

    @Test
    void changeMyPassword_shouldReturnMessageResponseWhenPasswordIsChangedSuccessfully() {

        ChangePasswordRequest request = ChangePasswordRequest.builder()
                .currentPassword(PASSWORD)
                .newPassword(NEW_PASSWORD)
                .confirmNewPassword(NEW_PASSWORD)
                .build();

        User existingUser = User.builder()
                .userId(USER_ID)
                .email(USER_EMAIL)
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
                .userId(USER_ID)
                .email(USER_EMAIL)
                .passwordHash(NEW_PASSWORD_HASH)
                .firstName("First Name")
                .lastName("Last Name")
                .userRole(USER_ROLE_CLIENT)
                .isEnabled(true)
                .isNonLocked(true)
                .registeredAt(TIMESTAMP_PAST)
                .updatedAt(TIMESTAMP_PAST)
                .build();

        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);

        when(userRepository.findByEmail(USER_EMAIL)).thenReturn(Optional.of(existingUser));
        when(passwordEncoder.matches(PASSWORD, PASSWORD_HASH)).thenReturn(true);
        when(passwordEncoder.encode(NEW_PASSWORD)).thenReturn(NEW_PASSWORD_HASH);
        when(userRepository.saveAndFlush(existingUser)).thenReturn(updatedUser);

        MessageResponse messageResponse = userService.changeMyPassword(USER_EMAIL, request);

        verify(userRepository, times(1)).findByEmail(USER_EMAIL);
        verify(passwordEncoder, times(1)).matches(PASSWORD, PASSWORD_HASH);
        verify(passwordEncoder, times(1)).encode(NEW_PASSWORD);
        verify(userRepository, times(1)).saveAndFlush(userCaptor.capture());

        User capturedUser = userCaptor.getValue();
        assertEquals(existingUser.getEmail(), capturedUser.getEmail());
        assertEquals(NEW_PASSWORD_HASH, capturedUser.getPasswordHash());
        assertEquals(existingUser.getFirstName(), capturedUser.getFirstName());
        assertEquals(existingUser.getLastName(), capturedUser.getLastName());

        assertNotNull(messageResponse);
        assertEquals(String.format("Password for user with email: %s, has been successfully changed.", USER_EMAIL), messageResponse.getMessage());
    }

    @Test
    void changeMyPassword_shouldThrowBadCredentialsExceptionWhenPasswordsMismatch() {

        ChangePasswordRequest request = ChangePasswordRequest.builder()
                .currentPassword(PASSWORD)
                .newPassword(NEW_PASSWORD)
                .confirmNewPassword("wrongPassword")
                .build();

        BadCredentialsException thrown = assertThrows(BadCredentialsException.class, () -> userService.changeMyPassword(USER_EMAIL, request));

        verify(userRepository, never()).findByEmail(anyString());
        verify(passwordEncoder, never()).matches(anyString(), anyString());
        verify(passwordEncoder, never()).encode(anyString());
        verify(userRepository, never()).saveAndFlush(any(User.class));

        assertEquals("Password doesn't match the CONFIRM NEW PASSWORD field.", thrown.getMessage());
    }

    @Test
    void changeMyPassword_shouldThrowDataNotFoundExceptionWhenUserNotFound() {

        ChangePasswordRequest request = ChangePasswordRequest.builder()
                .currentPassword(PASSWORD)
                .newPassword(NEW_PASSWORD)
                .confirmNewPassword(NEW_PASSWORD)
                .build();

        when(userRepository.findByEmail(NON_EXISTING_USER_EMAIL)).thenReturn(Optional.empty());

        DataNotFoundException thrown = assertThrows(DataNotFoundException.class, () ->
                userService.changeMyPassword(NON_EXISTING_USER_EMAIL, request));

        verify(userRepository, times(1)).findByEmail(NON_EXISTING_USER_EMAIL);
        verify(passwordEncoder, never()).matches(anyString(), anyString());
        verify(passwordEncoder, never()).encode(anyString());
        verify(userRepository, never()).saveAndFlush(any(User.class));

        assertEquals(String.format("User with email: %s, was not found.", NON_EXISTING_USER_EMAIL), thrown.getMessage());
    }

    @Test
    void changeMyPassword_shouldThrowBadCredentialsExceptionWhenMyPasswordDoes() {

        String invalidPassword = "Invalid Password";

        ChangePasswordRequest request = ChangePasswordRequest.builder()
                .currentPassword(invalidPassword)
                .newPassword(NEW_PASSWORD)
                .confirmNewPassword(NEW_PASSWORD)
                .build();

        User existingUser = User.builder()
                .userId(USER_ID)
                .email(USER_EMAIL)
                .passwordHash(PASSWORD_HASH)
                .firstName("First Name")
                .lastName("Last Name")
                .userRole(USER_ROLE_CLIENT)
                .isEnabled(true)
                .isNonLocked(true)
                .registeredAt(TIMESTAMP_PAST)
                .updatedAt(TIMESTAMP_PAST)
                .build();

        when(userRepository.findByEmail(USER_EMAIL)).thenReturn(Optional.of(existingUser));
        when(passwordEncoder.matches(invalidPassword, PASSWORD_HASH)).thenReturn(false);

        BadCredentialsException thrown = assertThrows(BadCredentialsException.class, () ->
                userService.changeMyPassword(USER_EMAIL, request)
        );

        verify(userRepository, times(1)).findByEmail(USER_EMAIL);
        verify(passwordEncoder, times(1)).matches(invalidPassword, PASSWORD_HASH);
        verify(passwordEncoder, never()).encode(anyString());
        verify(userRepository, never()).saveAndFlush(any(User.class));

        assertEquals("Given current password doesn't match the one in database.", thrown.getMessage());
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
    void setUserRole_shouldThrowUserLockedExceptionWhenUserIsLocked() {

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

        UserLockedException thrownException = assertThrows(UserLockedException.class, () -> userService.setUserRole(USER_ID.toString(), USER_ROLE_ADMIN.name()));

        verify(userRepository, times(1)).findById(USER_ID);
        verify(userRepository, never()).saveAndFlush(any(User.class));

        assertEquals(String.format("User with id: %s, is locked and his userRole can not be changed.", USER_ID), thrownException.getMessage());
    }

    @Test
    void setUserRole_shouldThrowUserDisabledExceptionWhenUserIsDisabled() {

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

        UserDisabledException thrownException = assertThrows(UserDisabledException.class, () -> userService.setUserRole(USER_ID.toString(), USER_ROLE_ADMIN.name()));

        verify(userRepository, times(1)).findById(USER_ID);
        verify(userRepository, never()).saveAndFlush(any(User.class));

        assertEquals(String.format("User with id: %s, is disabled and his userRole can not be changed.", USER_ID), thrownException.getMessage());
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
    void toggleUserLockState_shouldLockUserWhenUserIsEnabledAndNonLocked() {

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

        MessageResponse actualResponse = userService.toggleUserLockState(USER_ID.toString());

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
    void toggleUserLockState_shouldUnlockUserWhenUserIsEnabledAndLocked() {

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

        MessageResponse actualResponse = userService.toggleUserLockState(USER_ID.toString());

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
    void toggleUserLockState_shouldThrowIllegalArgumentExceptionWhenUserIdIsInvalidUuidString() {

        assertThrows(IllegalArgumentException.class, () -> userService.toggleUserLockState(INVALID_ID));

        verify(userRepository, never()).findById(any(UUID.class));
        verify(userMapper, never()).userToResponse(any(User.class));
    }

    @Test
    void toggleUserLockState_shouldThrowDataNotFoundExceptionWhenUserDoesNotExist() {

        when(userRepository.findById(NON_EXISTING_USER_ID)).thenReturn(Optional.empty());

        DataNotFoundException thrown = assertThrows(DataNotFoundException.class, () ->
                userService.toggleUserLockState(NON_EXISTING_USER_ID.toString()));

        verify(userRepository, times(1)).findById(NON_EXISTING_USER_ID);
        verify(userRepository, never()).saveAndFlush(any(User.class));

        assertEquals(String.format("User with id: %s, was not found.", NON_EXISTING_USER_ID), thrown.getMessage());
    }

    @Test
    void toggleUserLockState_shouldThrowUserDisabledExceptionWhenUserIsDisabled() {

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

        UserDisabledException thrown = assertThrows(UserDisabledException.class, () -> userService.toggleUserLockState(USER_ID.toString()));

        verify(userRepository, times(1)).findById(USER_ID);
        verify(userRepository, never()).saveAndFlush(any(User.class));

        assertEquals(String.format("User with id: %s, is disabled and can not be locked or unlocked.", USER_ID), thrown.getMessage());
    }

    @Test
    void unregisterMyAccount_shouldReturnMessageResponseWhenUnregisterIsSuccessful() {

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
                .firstName("Disabled User")
                .lastName("Disabled User")
                .userRole(existingUser.getUserRole())
                .isEnabled(false)
                .isNonLocked(existingUser.getIsNonLocked())
                .registeredAt(existingUser.getRegisteredAt())
                .updatedAt(TIMESTAMP_NOW)
                .build();

        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);

        when(userRepository.findByEmail(USER_EMAIL)).thenReturn(Optional.of(existingUser));
        when(passwordEncoder.matches(PASSWORD, PASSWORD_HASH)).thenReturn(true);
        when(userRepository.saveAndFlush(existingUser)).thenReturn(unregisteredUser);

        MessageResponse messageResponse = userService.unregisterMyAccount(USER_EMAIL, request);

        verify(userRepository, times(1)).findByEmail(USER_EMAIL);
        verify(passwordEncoder, times(1)).matches(PASSWORD, PASSWORD_HASH);
        verify(userRepository, times(1)).saveAndFlush(userCaptor.capture());

        User capturedUser = userCaptor.getValue();
        assertEquals(String.format("%s@example.com", USER_ID), capturedUser.getEmail());
        assertEquals("Disabled User", capturedUser.getFirstName());
        assertEquals("Disabled User", capturedUser.getLastName());
        assertNull(capturedUser.getRefreshToken());
        assertFalse(capturedUser.getIsEnabled());

        assertNotNull(messageResponse);
        assertEquals(String.format("User with email: %s, has been unregistered.", USER_EMAIL), messageResponse.getMessage());
    }

    @Test
    void unregisterMyAccount_shouldThrowBadCredentialsExceptionWhenPasswordsMismatch() {

        UserUnregisterRequest request = UserUnregisterRequest.builder()
                .password(PASSWORD)
                .confirmPassword("wrongPassword")
                .build();

        BadCredentialsException thrown = assertThrows(BadCredentialsException.class, () -> userService.unregisterMyAccount(USER_EMAIL, request));

        verify(userRepository, never()).findByEmail(anyString());
        verify(passwordEncoder, never()).matches(anyString(), anyString());
        verify(userRepository, never()).saveAndFlush(any(User.class));

        assertEquals("Password doesn't match the CONFIRM PASSWORD field.", thrown.getMessage());
    }

    @Test
    void unregisterMyAccount_shouldThrowDataNotFoundExceptionWhenUserNotFound() {

        UserUnregisterRequest request = UserUnregisterRequest.builder()
                .password(PASSWORD)
                .confirmPassword(PASSWORD)
                .build();

        when(userRepository.findByEmail(NON_EXISTING_USER_EMAIL)).thenReturn(Optional.empty());

        DataNotFoundException thrown = assertThrows(DataNotFoundException.class, () ->
                userService.unregisterMyAccount(NON_EXISTING_USER_EMAIL, request));

        verify(userRepository, times(1)).findByEmail(NON_EXISTING_USER_EMAIL);
        verify(passwordEncoder, never()).matches(anyString(), anyString());
        verify(userRepository, never()).saveAndFlush(any(User.class));

        assertEquals(String.format("User with email: %s, was not found.", NON_EXISTING_USER_EMAIL), thrown.getMessage());
    }

    @Test
    void unregisterMyAccount_shouldThrowBadCredentialsExceptionWhenPasswordIsIncorrect() {

        String incorrectPassword = "Incorrect Password";

        UserUnregisterRequest request = UserUnregisterRequest.builder()
                .password(incorrectPassword)
                .confirmPassword(incorrectPassword)
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

        when(userRepository.findByEmail(USER_EMAIL)).thenReturn(Optional.of(existingUser));
        when(passwordEncoder.matches(incorrectPassword, PASSWORD_HASH)).thenReturn(false);

        BadCredentialsException thrown = assertThrows(BadCredentialsException.class, () ->
                userService.unregisterMyAccount(USER_EMAIL, request)
        );

        verify(userRepository, times(1)).findByEmail(USER_EMAIL);
        verify(passwordEncoder, times(1)).matches(incorrectPassword, PASSWORD_HASH);
        verify(userRepository, never()).saveAndFlush(any(User.class));

        assertEquals("Given password doesn't match the password saved in database.", thrown.getMessage());
    }
}