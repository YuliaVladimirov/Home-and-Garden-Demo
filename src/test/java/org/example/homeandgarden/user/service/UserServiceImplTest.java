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
import org.springframework.data.web.PagedModel;
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
    private final String USER_ID_STRING = USER_ID.toString();
    private final UUID NON_EXISTING_USER_ID = UUID.randomUUID();
    private final String NON_EXISTING_USER_ID_STRING = NON_EXISTING_USER_ID.toString();
    private final String INVALID_USER_ID = "Invalid UUID";

    private final UserRole USER_ROLE_CLIENT = UserRole.CLIENT;
    private final UserRole USER_ROLE_ADMIN = UserRole.ADMINISTRATOR;
    private final String USER_ROLE_ADMIN_STRING = USER_ROLE_ADMIN.toString();

    private final String PASSWORD = "Raw Password";
    private final String PASSWORD_HASH = "Hashed Password";

    private final Instant UPDATED_AT_NOW = Instant.now();
    private final Instant REGISTERED_AT_PAST = Instant.now().minus(10L, ChronoUnit.DAYS);
    private final Instant UPDATED_AT_PAST = Instant.now().minus(10L, ChronoUnit.DAYS);

    UserUpdateRequest createUserUpdateRequest(String firstName, String lastName) {
        return UserUpdateRequest.builder()
                .firstName(firstName)
                .lastName(lastName)
                .build();
    }

    private User createUser(UUID id, String email, String passwordHash, String firstName, String lastName, UserRole userRole, Boolean isEnabled,Boolean isNonLocked, Instant registeredAt, Instant updatedAt) {
        return User.builder()
                .userId(id)
                .email(email)
                .passwordHash(passwordHash)
                .firstName(firstName)
                .lastName(lastName)
                .userRole(userRole)
                .isEnabled(isEnabled)
                .isNonLocked(isNonLocked)
                .registeredAt(registeredAt)
                .updatedAt(updatedAt)
                .build();
    }

    private UserResponse createUserResponse(UUID id, String email, String firstName, String lastName, UserRole userRole, Instant registeredAt, Instant updatedAt) {
        return UserResponse.builder()
                .userId(id)
                .email(email)
                .firstName(firstName)
                .lastName(lastName)
                .userRole(userRole)
                .registeredAt(registeredAt)
                .updatedAt(updatedAt)
                .build();
    }

    private UserResponse createUserResponseDetailed(UUID id, String email, String firstName, String lastName, UserRole userRole, Boolean isEnabled, Boolean isNonLocked, Instant registeredAt, Instant updatedAt) {
        return UserResponse.builder()
                .userId(id)
                .email(email)
                .firstName(firstName)
                .lastName(lastName)
                .userRole(userRole)
                .isEnabled(isEnabled)
                .isNonLocked(isNonLocked)
                .registeredAt(registeredAt)
                .updatedAt(updatedAt)
                .build();
    }



    @Test
    void getUsersByStatus_shouldReturnPagedModelOfEnabledAndNonLockedUsers() {

        Boolean isEnabled = true;
        Boolean isNonLocked = true;

        Pageable pageRequest = PageRequest.of(PAGE, SIZE, Sort.Direction.fromString(ORDER), SORT_BY);

        User user1 = createUser(USER_1_ID, "Email One ", "Password One", "First Name One", "Last Name One", USER_ROLE_CLIENT, true,true, REGISTERED_AT_PAST, UPDATED_AT_PAST);

        User user2 = createUser(USER_2_ID, "Email Two", "Password Two", "First Name Two", "Last Name Two", USER_ROLE_CLIENT, true,true, REGISTERED_AT_PAST, UPDATED_AT_PAST);

        List<User> allUsers = List.of(user1, user2);
        Page<User> userPage = new PageImpl<>(allUsers, pageRequest, allUsers.size());
        long expectedTotalPages = (long) Math.ceil((double) allUsers.size() / SIZE);

        UserResponse userResponseDetailed1 = createUserResponseDetailed(user1.getUserId(), user1.getEmail(), user1.getFirstName(), user1.getLastName(), user1.getUserRole(),  user1.getIsEnabled(), user1.getIsNonLocked(), user1.getRegisteredAt(), user1.getUpdatedAt());
        UserResponse userResponseDetailed2 = createUserResponseDetailed(user2.getUserId(), user2.getEmail(), user2.getFirstName(), user2.getLastName(), user2.getUserRole(), user2.getIsEnabled(), user2.getIsNonLocked(), user2.getRegisteredAt(), user2.getUpdatedAt());

        when(userRepository.findAllByIsEnabledAndIsNonLocked(isEnabled, isNonLocked, pageRequest)).thenReturn(userPage);
        when(userMapper.userToResponseDetailed(user1)).thenReturn(userResponseDetailed1);
        when(userMapper.userToResponseDetailed(user2)).thenReturn(userResponseDetailed2);

        PagedModel<UserResponse> actualResponse = userService.getUsersByStatus(isEnabled, isNonLocked, SIZE, PAGE, ORDER, SORT_BY);

        verify(userRepository, times(1)).findAllByIsEnabledAndIsNonLocked(isEnabled, isNonLocked, pageRequest);
        verify(userMapper, times(1)).userToResponseDetailed(user1);
        verify(userMapper, times(1)).userToResponseDetailed(user2);

        assertNotNull(actualResponse);
        assertNotNull(actualResponse.getMetadata());
        assertEquals(allUsers.size(), actualResponse.getMetadata().totalElements());
        assertEquals(expectedTotalPages, actualResponse.getMetadata().totalPages());
        assertEquals((long)SIZE, actualResponse.getMetadata().size());
        assertEquals((long)PAGE, actualResponse.getMetadata().number());

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

        User user1 = createUser(USER_1_ID, "Email One ", "Password One", "First Name One", "Last Name One", USER_ROLE_CLIENT, false,true, REGISTERED_AT_PAST, UPDATED_AT_PAST);

        User user2 = createUser(USER_2_ID, "Email Two", "Password Two", "First Name Two", "Last Name Two", USER_ROLE_CLIENT, false,true, REGISTERED_AT_PAST, UPDATED_AT_PAST);

        List<User> allUsers = List.of(user1, user2);
        Page<User> userPage = new PageImpl<>(allUsers, pageRequest, allUsers.size());
        long expectedTotalPages = (long) Math.ceil((double) allUsers.size() / SIZE);

        UserResponse userResponseDetailed1 = createUserResponseDetailed(user1.getUserId(), user1.getEmail(), user1.getFirstName(), user1.getLastName(), user1.getUserRole(),  user1.getIsEnabled(), user1.getIsNonLocked(), user1.getRegisteredAt(), user1.getUpdatedAt());
        UserResponse userResponseDetailed2 = createUserResponseDetailed(user2.getUserId(), user2.getEmail(), user2.getFirstName(), user2.getLastName(), user2.getUserRole(), user2.getIsEnabled(), user2.getIsNonLocked(), user2.getRegisteredAt(), user2.getUpdatedAt());

        when(userRepository.findAllByIsEnabledAndIsNonLocked(isEnabled, isNonLocked, pageRequest)).thenReturn(userPage);
        when(userMapper.userToResponseDetailed(user1)).thenReturn(userResponseDetailed1);
        when(userMapper.userToResponseDetailed(user2)).thenReturn(userResponseDetailed2);

        PagedModel<UserResponse> actualResponse = userService.getUsersByStatus(isEnabled, isNonLocked, SIZE, PAGE, ORDER, SORT_BY);

        verify(userRepository, times(1)).findAllByIsEnabledAndIsNonLocked(isEnabled, isNonLocked, pageRequest);
        verify(userMapper, times(1)).userToResponseDetailed(user1);
        verify(userMapper, times(1)).userToResponseDetailed(user2);

        assertNotNull(actualResponse);
        assertNotNull(actualResponse.getMetadata());
        assertEquals(allUsers.size(), actualResponse.getMetadata().totalElements());
        assertEquals(expectedTotalPages, actualResponse.getMetadata().totalPages());
        assertEquals((long)SIZE, actualResponse.getMetadata().size());
        assertEquals((long)PAGE, actualResponse.getMetadata().number());

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

        User user1 = createUser(USER_1_ID, "Email One ", "Password One", "First Name One", "Last Name One", USER_ROLE_CLIENT, true,false, REGISTERED_AT_PAST, UPDATED_AT_PAST);

        User user2 = createUser(USER_2_ID, "Email Two", "Password Two", "First Name Two", "Last Name Two", USER_ROLE_CLIENT, true,false, REGISTERED_AT_PAST, UPDATED_AT_PAST);

        List<User> allUsers = List.of(user1, user2);
        Page<User> userPage = new PageImpl<>(allUsers, pageRequest, allUsers.size());
        long expectedTotalPages = (long) Math.ceil((double) allUsers.size() / SIZE);

        UserResponse userResponseDetailed1 = createUserResponseDetailed(user1.getUserId(), user1.getEmail(), user1.getFirstName(), user1.getLastName(), user1.getUserRole(),  user1.getIsEnabled(), user1.getIsNonLocked(), user1.getRegisteredAt(), user1.getUpdatedAt());
        UserResponse userResponseDetailed2 = createUserResponseDetailed(user2.getUserId(), user2.getEmail(), user2.getFirstName(), user2.getLastName(), user2.getUserRole(), user2.getIsEnabled(), user2.getIsNonLocked(), user2.getRegisteredAt(), user2.getUpdatedAt());

        when(userRepository.findAllByIsEnabledAndIsNonLocked(isEnabled, isNonLocked, pageRequest)).thenReturn(userPage);
        when(userMapper.userToResponseDetailed(user1)).thenReturn(userResponseDetailed1);
        when(userMapper.userToResponseDetailed(user2)).thenReturn(userResponseDetailed2);

        PagedModel<UserResponse> actualResponse = userService.getUsersByStatus(isEnabled, isNonLocked, SIZE, PAGE, ORDER, SORT_BY);

        verify(userRepository, times(1)).findAllByIsEnabledAndIsNonLocked(isEnabled, isNonLocked, pageRequest);
        verify(userMapper, times(1)).userToResponseDetailed(user1);
        verify(userMapper, times(1)).userToResponseDetailed(user2);

        assertNotNull(actualResponse);
        assertNotNull(actualResponse.getMetadata());
        assertEquals(allUsers.size(), actualResponse.getMetadata().totalElements());
        assertEquals(expectedTotalPages, actualResponse.getMetadata().totalPages());
        assertEquals((long)SIZE, actualResponse.getMetadata().size());
        assertEquals((long)PAGE, actualResponse.getMetadata().number());

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

        User user1 = createUser(USER_1_ID, "Email One ", "Password One", "First Name One", "Last Name One", USER_ROLE_CLIENT, false,false, REGISTERED_AT_PAST, UPDATED_AT_PAST);

        User user2 = createUser(USER_2_ID, "Email Two", "Password Two", "First Name Two", "Last Name Two", USER_ROLE_CLIENT, false,false, REGISTERED_AT_PAST, UPDATED_AT_PAST);

        List<User> allUsers = List.of(user1, user2);
        Page<User> userPage = new PageImpl<>(allUsers, pageRequest, allUsers.size());
        long expectedTotalPages = (long) Math.ceil((double) allUsers.size() / SIZE);

        UserResponse userResponseDetailed1 = createUserResponseDetailed(user1.getUserId(), user1.getEmail(), user1.getFirstName(), user1.getLastName(), user1.getUserRole(),  user1.getIsEnabled(), user1.getIsNonLocked(), user1.getRegisteredAt(), user1.getUpdatedAt());
        UserResponse userResponseDetailed2 = createUserResponseDetailed(user2.getUserId(), user2.getEmail(), user2.getFirstName(), user2.getLastName(), user2.getUserRole(), user2.getIsEnabled(), user2.getIsNonLocked(), user2.getRegisteredAt(), user2.getUpdatedAt());

        when(userRepository.findAllByIsEnabledAndIsNonLocked(isEnabled, isNonLocked, pageRequest)).thenReturn(userPage);
        when(userMapper.userToResponseDetailed(user1)).thenReturn(userResponseDetailed1);
        when(userMapper.userToResponseDetailed(user2)).thenReturn(userResponseDetailed2);

        PagedModel<UserResponse> actualResponse = userService.getUsersByStatus(isEnabled, isNonLocked, SIZE, PAGE, ORDER, SORT_BY);

        verify(userRepository, times(1)).findAllByIsEnabledAndIsNonLocked(isEnabled, isNonLocked, pageRequest);
        verify(userMapper, times(1)).userToResponseDetailed(user1);
        verify(userMapper, times(1)).userToResponseDetailed(user2);

        assertNotNull(actualResponse);
        assertNotNull(actualResponse.getMetadata());
        assertEquals(allUsers.size(), actualResponse.getMetadata().totalElements());
        assertEquals(expectedTotalPages, actualResponse.getMetadata().totalPages());
        assertEquals((long)SIZE, actualResponse.getMetadata().size());
        assertEquals((long)PAGE, actualResponse.getMetadata().number());

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

        PagedModel<UserResponse> actualResponse = userService.getUsersByStatus(isEnabled, isNonLocked, SIZE, PAGE, ORDER, SORT_BY);

        verify(userRepository, times(1)).findAllByIsEnabledAndIsNonLocked(isEnabled, isNonLocked, pageRequest);
        verify(userMapper, never()).userToResponseDetailed(any(User.class));

        assertNotNull(actualResponse);
        assertNotNull(actualResponse.getMetadata());
        assertEquals(0L, actualResponse.getMetadata().totalElements());
        assertEquals(0L, actualResponse.getMetadata().totalPages());
        assertEquals((long)PAGE, actualResponse.getMetadata().number());
        assertEquals((long)SIZE, actualResponse.getMetadata().size());

        assertNotNull(actualResponse.getContent());
        assertTrue(actualResponse.getContent().isEmpty());
        assertEquals(0, actualResponse.getContent().size());
    }

    @Test
    void getUserById_shouldReturnUserResponseWhenUserExists() {

        User user = createUser(USER_ID, "Email", "Password", "First Name", "Last Name", USER_ROLE_CLIENT, true,true, REGISTERED_AT_PAST, UPDATED_AT_PAST);

        UserResponse userResponse = createUserResponse(user.getUserId(), user.getEmail(), user.getFirstName(), user.getLastName(), user.getUserRole(),  user.getRegisteredAt(), user.getUpdatedAt());

        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user));
        when(userMapper.userToResponse(user)).thenReturn(userResponse);

        UserResponse actualResponse = userService.getUserById(USER_ID_STRING);

        verify(userRepository, times(1)).findById(USER_ID);
        verify(userMapper, times(1)).userToResponse(user);

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
                userService.getUserById(NON_EXISTING_USER_ID_STRING));

        verify(userRepository, times(1)).findById(NON_EXISTING_USER_ID);
        verify(userMapper, never()).userToResponse(any(User.class));

        assertEquals(String.format("User with id: %s, was not found.", NON_EXISTING_USER_ID_STRING), thrownException.getMessage());
    }

    @Test
    void getUserById_shouldThrowIllegalArgumentExceptionWhenUserIdIsInvalidUuidString() {

        assertThrows(IllegalArgumentException.class, () -> userService.getUserById(INVALID_USER_ID));

        verify(userRepository, never()).findById(any(UUID.class));
        verify(userMapper, never()).userToResponse(any(User.class));
    }


    @Test
    void updateUser_shouldUpdateUserSuccessfullyWhenUserExistsAndIsEnabledAndNonLocked() {

        UserUpdateRequest updateRequest = createUserUpdateRequest("Updated First Name",  "Updated Last Name");

        User existingUser = createUser(USER_ID, "Original Email", "Original Password", "Original First Name", "Original Last Name", USER_ROLE_CLIENT, true,true, REGISTERED_AT_PAST, UPDATED_AT_PAST);

        User updatedUser = createUser(existingUser.getUserId(), existingUser.getEmail(), existingUser.getPasswordHash(), updateRequest.getFirstName(), updateRequest.getLastName(), existingUser.getUserRole(), existingUser.getIsEnabled(),existingUser.getIsNonLocked(), existingUser.getRegisteredAt(), UPDATED_AT_NOW);

        UserResponse userResponse = createUserResponse(updatedUser.getUserId(), updatedUser.getEmail(), updatedUser.getFirstName(), updatedUser.getLastName(), updatedUser.getUserRole(),  updatedUser.getRegisteredAt(), updatedUser.getUpdatedAt());

        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);

        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(existingUser));
        when(userRepository.saveAndFlush(existingUser)).thenReturn(updatedUser);
        when(userMapper.userToResponse(updatedUser)).thenReturn(userResponse);

        UserResponse actualResponse = userService.updateUser(USER_ID_STRING, updateRequest);

        verify(userRepository, times(1)).findById(USER_ID);

        verify(userRepository, times(1)).saveAndFlush(userCaptor.capture());
        User capturedUser = userCaptor.getValue();
        assertNotNull(capturedUser);
        assertEquals(existingUser.getUserId(), capturedUser.getUserId());
        assertEquals(existingUser.getEmail(), capturedUser.getEmail());
        assertEquals(existingUser.getPasswordHash(), capturedUser.getPasswordHash());
        assertEquals(updateRequest.getFirstName(), capturedUser.getFirstName());
        assertEquals(updateRequest.getLastName(), capturedUser.getLastName());
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

        UserUpdateRequest updateRequest = createUserUpdateRequest("Updated First Name",  null);

        User existingUser = createUser(USER_ID, "Original Email", "Original Password", "Original First Name", "Original Last Name", USER_ROLE_CLIENT, true,true, REGISTERED_AT_PAST, UPDATED_AT_PAST);

        User updatedUser = createUser(existingUser.getUserId(), existingUser.getEmail(), existingUser.getPasswordHash(), updateRequest.getFirstName(), existingUser.getLastName(), existingUser.getUserRole(), existingUser.getIsEnabled(),existingUser.getIsNonLocked(), existingUser.getRegisteredAt(), UPDATED_AT_NOW);

        UserResponse userResponse = createUserResponse(updatedUser.getUserId(), updatedUser.getEmail(), updatedUser.getFirstName(), updatedUser.getLastName(), updatedUser.getUserRole(),  updatedUser.getRegisteredAt(), updatedUser.getUpdatedAt());

        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);

        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(existingUser));
        when(userRepository.saveAndFlush(existingUser)).thenReturn(updatedUser);
        when(userMapper.userToResponse(updatedUser)).thenReturn(userResponse);

        UserResponse actualResponse = userService.updateUser(USER_ID_STRING, updateRequest);

        verify(userRepository, times(1)).findById(USER_ID);

        verify(userRepository, times(1)).saveAndFlush(userCaptor.capture());
        User capturedUser = userCaptor.getValue();
        assertNotNull(capturedUser);
        assertEquals(existingUser.getUserId(), capturedUser.getUserId());
        assertEquals(existingUser.getEmail(), capturedUser.getEmail());
        assertEquals(existingUser.getPasswordHash(), capturedUser.getPasswordHash());
        assertEquals(updateRequest.getFirstName(), capturedUser.getFirstName());
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

        UserUpdateRequest updateRequest = createUserUpdateRequest("Updated First Name",  "Updated Last Name");

        when(userRepository.findById(NON_EXISTING_USER_ID)).thenReturn(Optional.empty());

        DataNotFoundException thrownException = assertThrows(DataNotFoundException.class, () -> userService.updateUser(NON_EXISTING_USER_ID_STRING, updateRequest));

        verify(userRepository, times(1)).findById(NON_EXISTING_USER_ID);
        verify(userRepository, never()).saveAndFlush(any(User.class));
        verify(userMapper, never()).userToResponse(any(User.class));

        assertEquals(String.format("User with id: %s, was not found.", NON_EXISTING_USER_ID_STRING), thrownException.getMessage());
    }

    @Test
    void updateUser_shouldThrowIllegalArgumentExceptionWhenUserIdIsInvalidUuidString() {

        UserUpdateRequest updateRequest = createUserUpdateRequest("Updated First Name",  "Updated Last Name");

        assertThrows(IllegalArgumentException.class, () -> userService.updateUser(INVALID_USER_ID, updateRequest));

        verify(userRepository, never()).findById(any(UUID.class));
        verify(userRepository, never()).saveAndFlush(any(User.class));
        verify(userMapper, never()).userToResponse(any(User.class));
    }

    @Test
    void updateUser_shouldThrowIllegalArgumentExceptionWhenUserIsDisabled() {

        UserUpdateRequest updateRequest = createUserUpdateRequest("Updated First Name",  "Updated Last Name");

        User existingUser = createUser(USER_ID, "Original Email", "Original Password", "Original First Name", "Original Last Name", USER_ROLE_CLIENT, false,true, REGISTERED_AT_PAST, UPDATED_AT_PAST);

        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(existingUser));

        IllegalArgumentException thrownException = assertThrows(IllegalArgumentException.class, () -> userService.updateUser(USER_ID_STRING, updateRequest));

        verify(userRepository, times(1)).findById(USER_ID);
        verify(userRepository, never()).saveAndFlush(any(User.class));
        verify(userMapper, never()).userToResponse(any(User.class));

        assertEquals(String.format("User with id: %s, is unregistered and can not be updated.", USER_ID_STRING), thrownException.getMessage());
    }

    @Test
    void updateUser_shouldThrowIllegalArgumentExceptionWhenUserIsLocked() {

        UserUpdateRequest updateRequest = createUserUpdateRequest("Updated First Name",  "Updated Last Name");

        User existingUser = createUser(USER_ID, "Original Email", "Original Password", "Original First Name", "Original Last Name", USER_ROLE_CLIENT, true,false, REGISTERED_AT_PAST, UPDATED_AT_PAST);

        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(existingUser));

        IllegalArgumentException thrownException = assertThrows(IllegalArgumentException.class, () -> userService.updateUser(USER_ID_STRING, updateRequest));

        verify(userRepository, times(1)).findById(USER_ID);
        verify(userRepository, never()).saveAndFlush(any(User.class));
        verify(userMapper, never()).userToResponse(any(User.class));

        assertEquals(String.format("User with id: %s, is locked and can not be updated.", USER_ID_STRING), thrownException.getMessage());
    }

    @Test
    void setUserRole_shouldSetUserRoleSuccessfullyWhenUserExistsAndIsEnabledAndNonLocked() {

        User existingUser = createUser(USER_ID, "Original Email", "Original Password", "Original First Name", "Original Last Name", USER_ROLE_CLIENT, true,true, REGISTERED_AT_PAST, UPDATED_AT_PAST);

        User updatedUser = createUser(existingUser.getUserId(), existingUser.getEmail(), existingUser.getPasswordHash(), existingUser.getFirstName(), existingUser.getLastName(), USER_ROLE_ADMIN, existingUser.getIsEnabled(),existingUser.getIsNonLocked(), existingUser.getRegisteredAt(), UPDATED_AT_NOW);

        MessageResponse messageResponse = MessageResponse.builder()
                .message(String.format("UserRole %s was set for user with id: %s.", USER_ROLE_ADMIN_STRING, USER_ID_STRING))
                .build();

        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);

        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(existingUser));
        when(userRepository.saveAndFlush(existingUser)).thenReturn(updatedUser);

        MessageResponse actualResponse = userService.setUserRole(USER_ID_STRING, USER_ROLE_ADMIN_STRING);

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

        assertThrows(IllegalArgumentException.class, () -> userService.setUserRole(INVALID_USER_ID, USER_ROLE_ADMIN_STRING));

        verify(userRepository, never()).findById(any(UUID.class));
        verify(userRepository, never()).saveAndFlush(any(User.class));
        verify(userMapper, never()).userToResponse(any(User.class));
    }

    @Test
    void setUserRole_shouldThrowDataNotFoundExceptionWhenUserDoesNotExist() {

        when(userRepository.findById(NON_EXISTING_USER_ID)).thenReturn(Optional.empty());

        DataNotFoundException thrownException = assertThrows(DataNotFoundException.class, () -> userService.setUserRole(NON_EXISTING_USER_ID_STRING, USER_ROLE_ADMIN_STRING));

        verify(userRepository, times(1)).findById(NON_EXISTING_USER_ID);
        verify(userRepository, never()).saveAndFlush(any(User.class));

        assertEquals(String.format("User with id: %s, was not found.", NON_EXISTING_USER_ID_STRING), thrownException.getMessage());
    }

    @Test
    void setUserRole_shouldThrowIllegalArgumentExceptionWhenUserIsLocked() {

        User existingUser = createUser(USER_ID, "Original Email", "Original Password", "Original First Name", "Original Last Name", USER_ROLE_CLIENT, true,false, REGISTERED_AT_PAST, UPDATED_AT_PAST);

        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(existingUser));

        IllegalArgumentException thrownException = assertThrows(IllegalArgumentException.class, () -> userService.setUserRole(USER_ID_STRING, USER_ROLE_ADMIN_STRING));

        verify(userRepository, times(1)).findById(USER_ID);
        verify(userRepository, never()).saveAndFlush(any(User.class));

        assertEquals(String.format("User with id: %s, is locked and his userRole can not be changed.", USER_ID_STRING), thrownException.getMessage());
    }

    @Test
    void setUserRole_shouldThrowIllegalArgumentExceptionWhenUserIsDisabled() {

        User existingUser = createUser(USER_ID, "Original Email", "Original Password", "Original First Name", "Original Last Name", USER_ROLE_CLIENT, false,true, REGISTERED_AT_PAST, UPDATED_AT_PAST);

        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(existingUser));

        IllegalArgumentException thrownException = assertThrows(IllegalArgumentException.class, () -> userService.setUserRole(USER_ID_STRING, USER_ROLE_ADMIN_STRING));

        verify(userRepository, times(1)).findById(USER_ID);
        verify(userRepository, never()).saveAndFlush(any(User.class));

        assertEquals(String.format("User with id: %s, is unregistered and his userRole can not be changed.", USER_ID_STRING), thrownException.getMessage());
    }

    @Test
    void setUserRole_shouldThrowIllegalArgumentExceptionWhenUserHasAlreadyHasTargetRole() {

        String sameRole = UserRole.CLIENT.toString();

        User existingUser = createUser(USER_ID, "Original Email", "Original Password", "Original First Name", "Original Last Name", USER_ROLE_CLIENT, true,true, REGISTERED_AT_PAST, UPDATED_AT_PAST);

        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(existingUser));

        IllegalArgumentException thrownException = assertThrows(IllegalArgumentException.class, () -> userService.setUserRole(USER_ID_STRING, sameRole));

        verify(userRepository, times(1)).findById(USER_ID);
        verify(userRepository, never()).saveAndFlush(any(User.class));

        assertEquals(String.format("User with id: %s, already has userRole '%s'.", USER_ID_STRING, sameRole), thrownException.getMessage());
    }

    @Test
    void setUserRole_shouldThrowIllegalStateExceptionWhenRoleUpdateFailsOnSave() {

        User existingUser = createUser(USER_ID, "Original Email", "Original Password", "Original First Name", "Original Last Name", USER_ROLE_CLIENT, true,true, REGISTERED_AT_PAST, UPDATED_AT_PAST);

        User userWithOriginalRole = createUser(USER_ID, "Original Email", "Original Password", "Original First Name", "Original Last Name", USER_ROLE_CLIENT, true,true, REGISTERED_AT_PAST, UPDATED_AT_NOW);

        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);

        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(existingUser));
        when(userRepository.saveAndFlush(existingUser)).thenReturn(userWithOriginalRole);

        IllegalStateException thrownException = assertThrows(IllegalStateException.class, () ->
                userService.setUserRole(USER_ID_STRING, USER_ROLE_ADMIN_STRING));

        verify(userRepository, times(1)).findById(USER_ID);
        verify(userRepository, times(1)).saveAndFlush(userCaptor.capture());
        User capturedUser = userCaptor.getValue();
        assertNotNull(capturedUser);
        assertEquals(existingUser.getUserId(), capturedUser.getUserId());
        assertEquals(existingUser.getUserId(), capturedUser.getUserId());
        assertEquals(existingUser.getEmail(), capturedUser.getEmail());
        assertEquals(existingUser.getPasswordHash(), capturedUser.getPasswordHash());
        assertEquals(existingUser.getFirstName(), capturedUser.getFirstName());
        assertEquals(existingUser.getLastName(), capturedUser.getLastName());
        assertEquals(USER_ROLE_ADMIN, capturedUser.getUserRole());

        assertEquals(String.format("Unfortunately something went wrong and userRole '%s' was not set for user with id: %s. Please, try again.", USER_ROLE_ADMIN_STRING, USER_ID_STRING), thrownException.getMessage());
    }

    @Test
    void toggleLockState_shouldLockUser_whenUserIsEnabledAndNonLocked() {

        User existingUser = createUser(USER_ID, "Original Email", "Original Password", "Original First Name", "Original Last Name", USER_ROLE_CLIENT, true,true, REGISTERED_AT_PAST, UPDATED_AT_PAST);

        User updatedUser = createUser(existingUser.getUserId(), existingUser.getEmail(), existingUser.getPasswordHash(), existingUser.getFirstName(), existingUser.getLastName(), existingUser.getUserRole(), existingUser.getIsEnabled(),!existingUser.getIsNonLocked(), existingUser.getRegisteredAt(), UPDATED_AT_NOW);

        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);

        MessageResponse messageResponse = MessageResponse.builder()
                .message(String.format("User with id: %s has been %s.", USER_ID_STRING, existingUser.getIsNonLocked() ? "locked" : "unlocked"))
                .build();

        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(existingUser));
        when(userRepository.saveAndFlush(existingUser)).thenReturn(updatedUser);

        MessageResponse actualResponse = userService.toggleLockState(USER_ID_STRING);

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
    void toggleLockState_shouldUnlockUser_whenUserIsEnabledAndLocked() {

        User existingUser = createUser(USER_ID, "Original Email", "Original Password", "Original First Name", "Original Last Name", USER_ROLE_CLIENT, true,false, REGISTERED_AT_PAST, UPDATED_AT_PAST);

        User updatedUser = createUser(existingUser.getUserId(), existingUser.getEmail(), existingUser.getPasswordHash(), existingUser.getFirstName(), existingUser.getLastName(), existingUser.getUserRole(), existingUser.getIsEnabled(),!existingUser.getIsNonLocked(), existingUser.getRegisteredAt(), UPDATED_AT_NOW);

        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);

        MessageResponse messageResponse = MessageResponse.builder()
                .message(String.format("User with id: %s has been %s.", USER_ID_STRING, existingUser.getIsNonLocked() ? "locked" : "unlocked"))
                .build();

        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(existingUser));
        when(userRepository.saveAndFlush(existingUser)).thenReturn(updatedUser);

        MessageResponse actualResponse = userService.toggleLockState(USER_ID_STRING);

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

        assertThrows(IllegalArgumentException.class, () -> userService.toggleLockState(INVALID_USER_ID));

        verify(userRepository, never()).findById(any(UUID.class));
        verify(userMapper, never()).userToResponse(any(User.class));
    }

    @Test
    void toggleLockState_shouldThrowDataNotFoundExceptionWhenUserDoesNotExist() {

        when(userRepository.findById(NON_EXISTING_USER_ID)).thenReturn(Optional.empty());

        DataNotFoundException thrown = assertThrows(DataNotFoundException.class, () ->
                userService.toggleLockState(NON_EXISTING_USER_ID_STRING));

        verify(userRepository, times(1)).findById(NON_EXISTING_USER_ID);
        verify(userRepository, never()).saveAndFlush(any(User.class));

        assertEquals(String.format("User with id: %s, was not found.", NON_EXISTING_USER_ID_STRING), thrown.getMessage());
    }

    @Test
    void toggleLockState_shouldThrowIllegalArgumentExceptionWhenUserIsDisabled() {

        User existingUser = createUser(USER_ID, "Original Email", "Original Password", "Original First Name", "Original Last Name", USER_ROLE_CLIENT, false,true, REGISTERED_AT_PAST, UPDATED_AT_PAST);

        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(existingUser));

        IllegalArgumentException thrown = assertThrows(IllegalArgumentException.class, () -> userService.toggleLockState(USER_ID_STRING));

        verify(userRepository, times(1)).findById(USER_ID);
        verify(userRepository, never()).saveAndFlush(any(User.class));

        assertEquals(String.format("User with id: %s, is unregistered and can not be locked or unlocked.", USER_ID_STRING), thrown.getMessage());
    }

    @Test
    void toggleLockState_shouldThrowIllegalStateExceptionWhenLockingFailsUnexpectedly() {

        User existingUser = createUser(USER_ID, "Original Email", "Original Password", "Original First Name", "Original Last Name", USER_ROLE_CLIENT, true,true, REGISTERED_AT_PAST, UPDATED_AT_PAST);

        User userWithOriginalLockState = createUser(existingUser.getUserId(), existingUser.getEmail(), existingUser.getPasswordHash(), existingUser.getFirstName(), existingUser.getLastName(), existingUser.getUserRole(), existingUser.getIsEnabled(),existingUser.getIsNonLocked(), existingUser.getRegisteredAt(), UPDATED_AT_NOW);

        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(existingUser));
        when(userRepository.saveAndFlush(existingUser)).thenReturn(userWithOriginalLockState);

        IllegalStateException thrown = assertThrows(IllegalStateException.class, () ->
                userService.toggleLockState(USER_ID_STRING));

        verify(userRepository, times(1)).findById(USER_ID);
        verify(userRepository, times(1)).saveAndFlush(existingUser);

        assertEquals(String.format("An error occurred while locking the user with id: %s. Please try again.", USER_ID_STRING), thrown.getMessage());
    }

    @Test
    void unregisterUser_shouldSucceedWhenUserIsValidAndNotLockedAndPasswordsMatch() {

        UserUnregisterRequest request = new UserUnregisterRequest(PASSWORD, PASSWORD);

        User existingUser = createUser(USER_ID, "Original Email", PASSWORD_HASH, "Original First Name", "Original Last Name", USER_ROLE_CLIENT, true,true, REGISTERED_AT_PAST, UPDATED_AT_PAST);

        User unregisteredUser = createUser(existingUser.getUserId(), String.format("%s@example.com", existingUser.getUserId()), PASSWORD_HASH, "Deleted User", "Deleted User", USER_ROLE_CLIENT, false,true, existingUser.getRegisteredAt(), UPDATED_AT_NOW);

        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);

        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(existingUser));
        when(passwordEncoder.matches(PASSWORD, PASSWORD_HASH)).thenReturn(true);
        when(userRepository.saveAndFlush(existingUser)).thenReturn(unregisteredUser);

        MessageResponse messageResponse = userService.unregisterUser(USER_ID_STRING, request);

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
        assertEquals(String.format("User with id: %s, has been unregistered.", USER_ID_STRING), messageResponse.getMessage());
    }

    @Test
    void unregisterUser_shouldThrowBadCredentialsExceptionWhenPasswordsMismatch() {

        UserUnregisterRequest request = new UserUnregisterRequest(PASSWORD, "wrongConfirmPassword");

        BadCredentialsException thrown = assertThrows(BadCredentialsException.class, () -> userService.unregisterUser(USER_ID_STRING, request));

        verify(userRepository, never()).findById(any(UUID.class));
        verify(passwordEncoder, never()).matches(anyString(), anyString());
        verify(userRepository, never()).saveAndFlush(any(User.class));

        assertEquals("Password doesn't match the CONFIRM PASSWORD field.", thrown.getMessage());
    }

    @Test
    void unregisterUser_shouldThrowIllegalArgumentExceptionWhenUserIdIsInvalidUuidString() {

        UserUnregisterRequest request = new UserUnregisterRequest(PASSWORD, PASSWORD);

        assertThrows(IllegalArgumentException.class, () -> userService.unregisterUser(INVALID_USER_ID, request));

        verify(userRepository, never()).findById(any(UUID.class));
        verify(passwordEncoder, never()).matches(anyString(), anyString());
        verify(userRepository, never()).saveAndFlush(any(User.class));
    }

    @Test
    void unregisterUser_shouldThrowDataNotFoundExceptionWhenUserNotFound() {

        UserUnregisterRequest request = new UserUnregisterRequest(PASSWORD, PASSWORD);

        when(userRepository.findById(NON_EXISTING_USER_ID)).thenReturn(Optional.empty());

        DataNotFoundException thrown = assertThrows(DataNotFoundException.class, () ->
                userService.unregisterUser(NON_EXISTING_USER_ID_STRING, request));

        verify(userRepository, times(1)).findById(NON_EXISTING_USER_ID);
        verify(passwordEncoder, never()).matches(anyString(), anyString());
        verify(userRepository, never()).saveAndFlush(any(User.class));

        assertEquals(String.format("User with id: %s, was not found.", NON_EXISTING_USER_ID_STRING), thrown.getMessage());
    }

    @Test
    void unregisterUser_shouldThrowBadCredentialsExceptionWhenPasswordIncorrect() {

        String invalidPassword = "INVALID_PASSWORD";
        UserUnregisterRequest request = new UserUnregisterRequest(invalidPassword, invalidPassword);

        User existingUser = createUser(USER_ID, "Original Email", PASSWORD_HASH, "Original First Name", "Original Last Name", USER_ROLE_CLIENT, true,true, REGISTERED_AT_PAST, UPDATED_AT_PAST);

        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(existingUser));
        when(passwordEncoder.matches(invalidPassword, PASSWORD_HASH)).thenReturn(false);

        BadCredentialsException thrown = assertThrows(BadCredentialsException.class, () ->
                userService.unregisterUser(USER_ID_STRING, request)
        );

        verify(userRepository, times(1)).findById(USER_ID);
        verify(passwordEncoder, times(1)).matches(invalidPassword, PASSWORD_HASH);
        verify(userRepository, never()).saveAndFlush(any(User.class));

        assertEquals("Given password doesn't match the password saved in database.", thrown.getMessage());
    }

    @Test
    void unregisterUser_shouldThrowIllegalArgumentExceptionWhenUserIsLocked() {

        UserUnregisterRequest request = new UserUnregisterRequest(PASSWORD, PASSWORD);

        User lockedUser = createUser(USER_ID, "Original Email", PASSWORD_HASH, "Original First Name", "Original Last Name", USER_ROLE_CLIENT, true,false, REGISTERED_AT_PAST, UPDATED_AT_PAST);

        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(lockedUser));
        when(passwordEncoder.matches(PASSWORD, PASSWORD_HASH)).thenReturn(true);

        IllegalArgumentException thrown = assertThrows(IllegalArgumentException.class, () ->
                userService.unregisterUser(USER_ID_STRING, request));

        verify(userRepository, times(1)).findById(USER_ID);
        verify(passwordEncoder, times(1)).matches(PASSWORD, PASSWORD_HASH);
        verify(userRepository, never()).saveAndFlush(any(User.class));

        assertEquals(String.format("User with id: %s, is locked and can not be unregistered.", USER_ID_STRING), thrown.getMessage());
    }

    @Test
    void unregisterUser_shouldThrowIllegalArgumentExceptionWhenUserIsAlreadyUnregistered() {

        UserUnregisterRequest request = new UserUnregisterRequest(PASSWORD, PASSWORD);

        User unregisteredUser = createUser(USER_ID, "Original Email", PASSWORD_HASH, "Original First Name", "Original Last Name", USER_ROLE_CLIENT, false,true, REGISTERED_AT_PAST, UPDATED_AT_PAST);

        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(unregisteredUser));
        when(passwordEncoder.matches(PASSWORD, PASSWORD_HASH)).thenReturn(true);

        IllegalArgumentException thrown = assertThrows(IllegalArgumentException.class, () ->
                userService.unregisterUser(USER_ID_STRING, request));

        verify(userRepository, times(1)).findById(USER_ID);
        verify(passwordEncoder, times(1)).matches(PASSWORD, PASSWORD_HASH);
        verify(userRepository, never()).saveAndFlush(any(User.class));

        assertEquals(String.format("User with id: %s, is already unregistered.", USER_ID_STRING), thrown.getMessage());
    }

    @Test
    void unregisterUser_shouldThrowIllegalStateExceptionWhenUnregisterFailsOnSave() {

        UserUnregisterRequest request = new UserUnregisterRequest(PASSWORD, PASSWORD);

        User existingUser = createUser(USER_ID, "Original Email", PASSWORD_HASH, "Original First Name", "Original Last Name", USER_ROLE_CLIENT, true,true, REGISTERED_AT_PAST, UPDATED_AT_PAST);

        User existingUserWithOriginals = createUser(USER_ID, "Original Email", PASSWORD_HASH, "Original First Name", "Original Last Name", USER_ROLE_CLIENT, true,true, REGISTERED_AT_PAST, UPDATED_AT_NOW);

        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);

        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(existingUser));
        when(passwordEncoder.matches(PASSWORD, PASSWORD_HASH)).thenReturn(true);
        when(userRepository.saveAndFlush(userCaptor.capture())).thenReturn(existingUserWithOriginals);

        IllegalStateException thrown = assertThrows(IllegalStateException.class, () ->
                userService.unregisterUser(USER_ID_STRING, request));

        verify(userRepository, times(1)).findById(USER_ID);
        verify(passwordEncoder, times(1)).matches(PASSWORD, PASSWORD_HASH);
        verify(userRepository, times(1)).saveAndFlush(any(User.class));
        User capturedUser = userCaptor.getValue();
        assertFalse(capturedUser.getIsEnabled());

        assertEquals(String.format("Unfortunately something went wrong and user with id: %s, was not unregistered. Please, try again.", USER_ID_STRING), thrown.getMessage());
    }
}