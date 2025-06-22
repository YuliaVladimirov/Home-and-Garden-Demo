package org.example.homeandgarden.security.service;

import org.example.homeandgarden.exception.DataAlreadyExistsException;
import org.example.homeandgarden.security.config.JwtService;
import org.example.homeandgarden.security.dto.LoginRequest;
import org.example.homeandgarden.security.dto.LoginResponse;
import org.example.homeandgarden.user.dto.UserRegisterRequest;
import org.example.homeandgarden.user.dto.UserResponse;
import org.example.homeandgarden.user.entity.User;
import org.example.homeandgarden.user.entity.enums.UserRole;
import org.example.homeandgarden.user.mapper.UserMapper;
import org.example.homeandgarden.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceImplTest {

    @Mock
    private AuthenticationManager authenticationManager;

    @Mock
    private JwtService jwtService;

    @Mock
    private UserRepository userRepository;

    @Mock
    private UserMapper userMapper;

    @InjectMocks
    private AuthServiceImpl authService;

    private final UUID USER_ID = UUID.randomUUID();
    private final String USER_ID_STRING = USER_ID.toString();
    private final UUID NON_EXISTING_USER_ID = UUID.randomUUID();
    private final String NON_EXISTING_USER_ID_STRING = NON_EXISTING_USER_ID.toString();
    private final String INVALID_USER_ID = "Invalid UUID";

    private final String EMAIL = "test@example.com";
    private final String PASSWORD = "Raw Password";
    private final String PASSWORD_HASH = "Hashed Password";

    private final String ACCESS_TOKEN = "Access Token";
    private final String REFRESH_TOKEN = "Refresh Token";

    private UserRegisterRequest createUserRegisterRequest(String email, String password, String confirmPassword, String firstName, String lastName) {
        return UserRegisterRequest.builder()
                .email(email)
                .password(password)
                .confirmPassword(confirmPassword)
                .firstName(firstName)
                .lastName(lastName)
                .build();
    }

    private LoginRequest createLoginRequest(String email, String password){
        return LoginRequest.builder()
                .email(email)
                .password(password)
                .build();
    }

    private User createUser(UUID id, String email, String passwordHash, String firstName, String lastName, UserRole userRole, Boolean isEnabled, Boolean isNonLocked, Instant registeredAt, Instant updatedAt, String refreshToken) {
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
                .refreshToken(refreshToken)
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

    @BeforeEach
    void setUp() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void registerUser_shouldRegisterUserSuccessfully_whenEmailDoesNotExistAndPasswordsMatch() {

        UserRegisterRequest userRegisterRequest = createUserRegisterRequest(EMAIL, PASSWORD, PASSWORD, "First Name", "Last Name");

        User userToRegister = createUser(null, userRegisterRequest.getEmail(), PASSWORD_HASH, userRegisterRequest.getFirstName(), userRegisterRequest.getLastName(), UserRole.CLIENT, true,true, null, null, null);

        User registeredUser = createUser(USER_ID, userToRegister.getEmail(), userToRegister.getPasswordHash(), userToRegister.getFirstName(), userToRegister.getLastName(), userToRegister.getUserRole(), userToRegister.getIsEnabled(),userToRegister.getIsNonLocked(), Instant.now(), Instant.now(), userToRegister.getRefreshToken());

        UserResponse userResponse = createUserResponse(registeredUser.getUserId(), registeredUser.getEmail(), registeredUser.getFirstName(), registeredUser.getLastName(), registeredUser.getUserRole(),  registeredUser.getRegisteredAt(), registeredUser.getUpdatedAt());

        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);

        when(userRepository.existsByEmail(userRegisterRequest.getEmail())).thenReturn(false);
        when(userMapper.createRequestToUser(userRegisterRequest)).thenReturn(userToRegister);
        when(userRepository.saveAndFlush(userCaptor.capture())).thenReturn(registeredUser);
        when(userMapper.userToResponse(registeredUser)).thenReturn(userResponse);

        UserResponse actualResponse = authService.registerUser(userRegisterRequest);

        verify(userRepository, times(1)).existsByEmail(userRegisterRequest.getEmail());
        verify(userRepository, never()).existsByEmailAndIsEnabledFalse(any(String.class));
        verify(userRepository, never()).existsByEmailAndIsNonLockedFalse(any(String.class));
        verify(userMapper, times(1)).createRequestToUser(userRegisterRequest);

        verify(userRepository, times(1)).saveAndFlush(userToRegister);
        User capturedUser = userCaptor.getValue();
        assertNotNull(capturedUser);
        assertEquals(userToRegister.getUserId(), capturedUser.getUserId());
        assertEquals(userToRegister.getEmail(), capturedUser.getEmail());
        assertEquals(userToRegister.getPasswordHash(), capturedUser.getPasswordHash());
        assertEquals(userToRegister.getFirstName(), capturedUser.getFirstName());
        assertEquals(userToRegister.getLastName(), capturedUser.getLastName());
        assertEquals(userToRegister.getUserRole(), capturedUser.getUserRole());
        assertEquals(userToRegister.getIsEnabled(), capturedUser.getIsEnabled());
        assertEquals(userToRegister.getIsNonLocked(), capturedUser.getIsNonLocked());

        verify(userMapper, times(1)).userToResponse(registeredUser);

        assertNotNull(actualResponse);
        assertEquals(userResponse.getUserId(), actualResponse.getUserId());
        assertEquals(userResponse.getEmail(), actualResponse.getEmail());
        assertEquals(userResponse.getFirstName(), actualResponse.getFirstName());
        assertEquals(userResponse.getLastName(), actualResponse.getLastName());
        assertEquals(userResponse.getUserRole(), actualResponse.getUserRole());
        assertEquals(userResponse.getRegisteredAt(), actualResponse.getRegisteredAt());
        assertEquals(userResponse.getUpdatedAt(), actualResponse.getUpdatedAt());
    }

    @Test
    void registerUser_shouldThrowBadCredentialsExceptionWhenPasswordsMismatch() {

        UserRegisterRequest userRegisterRequest = createUserRegisterRequest(EMAIL, PASSWORD, "Wrong Password", "First Name", "Last Name");

        BadCredentialsException thrownException = assertThrows(BadCredentialsException.class, () -> authService.registerUser(userRegisterRequest));

        verify(userRepository, never()).existsByEmail(any(String.class));
        verify(userRepository, never()).existsByEmailAndIsEnabledFalse(any(String.class));
        verify(userRepository, never()).existsByEmailAndIsNonLockedFalse(any(String.class));
        verify(userMapper, never()).createRequestToUser(any(UserRegisterRequest.class));
        verify(userRepository, never()).saveAndFlush(any(User.class));
        verify(userMapper, never()).userToResponse(any(User.class));

        assertEquals("Password doesn't match the CONFIRM PASSWORD field.", thrownException.getMessage());
    }

    @Test
    void registerUser_shouldThrowDataAlreadyExistsExceptionWhenUserAlreadyExists() {

        UserRegisterRequest userRegisterRequest = createUserRegisterRequest(EMAIL, PASSWORD, PASSWORD, "First Name", "Last Name");

        when(userRepository.existsByEmail(userRegisterRequest.getEmail())).thenReturn(true);
        when(userRepository.existsByEmailAndIsEnabledFalse(userRegisterRequest.getEmail())).thenReturn(false);
        when(userRepository.existsByEmailAndIsNonLockedFalse(userRegisterRequest.getEmail())).thenReturn(false);

        DataAlreadyExistsException thrownException = assertThrows(DataAlreadyExistsException.class, () -> authService.registerUser(userRegisterRequest));

        verify(userRepository, times(1)).existsByEmail(userRegisterRequest.getEmail());
        verify(userRepository, times(1)).existsByEmailAndIsEnabledFalse(any(String.class));
        verify(userRepository, times(1)).existsByEmailAndIsNonLockedFalse(any(String.class));
        verify(userMapper, never()).createRequestToUser(any(UserRegisterRequest.class));
        verify(userRepository, never()).saveAndFlush(any(User.class));
        verify(userMapper, never()).userToResponse(any(User.class));

        assertEquals(String.format("User with email: %s, already registered.", userRegisterRequest.getEmail()), thrownException.getMessage());

    }

    @Test
    void registerUser_shouldThrowDataAlreadyExistsExceptionWhenUserExistsAndIsDisabled() {

        UserRegisterRequest userRegisterRequest = createUserRegisterRequest(EMAIL, PASSWORD, PASSWORD, "First Name", "Last Name");

        when(userRepository.existsByEmail(userRegisterRequest.getEmail())).thenReturn(true);
        when(userRepository.existsByEmailAndIsEnabledFalse(userRegisterRequest.getEmail())).thenReturn(true);

        DataAlreadyExistsException thrownException = assertThrows(DataAlreadyExistsException.class, () -> authService.registerUser(userRegisterRequest));

        verify(userRepository, times(1)).existsByEmail(userRegisterRequest.getEmail());
        verify(userRepository, times(1)).existsByEmailAndIsEnabledFalse(any(String.class));
        verify(userRepository, never()).existsByEmailAndIsNonLockedFalse(any(String.class));
        verify(userMapper, never()).createRequestToUser(any(UserRegisterRequest.class));
        verify(userRepository, never()).saveAndFlush(any(User.class));
        verify(userMapper, never()).userToResponse(any(User.class));

        assertEquals(String.format("User with email: %s, already exists and is disabled.", userRegisterRequest.getEmail()), thrownException.getMessage());
    }

    @Test
    void registerUser_shouldThrowDataAlreadyExistsExceptionWhenUserExistsAndIsLocked() {
        UserRegisterRequest userRegisterRequest = createUserRegisterRequest(EMAIL, PASSWORD, PASSWORD, "First Name", "Last Name");

        when(userRepository.existsByEmail(userRegisterRequest.getEmail())).thenReturn(true);
        when(userRepository.existsByEmailAndIsEnabledFalse(userRegisterRequest.getEmail())).thenReturn(false);
        when(userRepository.existsByEmailAndIsNonLockedFalse(userRegisterRequest.getEmail())).thenReturn(true);

        DataAlreadyExistsException thrownException = assertThrows(DataAlreadyExistsException.class, () -> authService.registerUser(userRegisterRequest));

        verify(userRepository, times(1)).existsByEmail(userRegisterRequest.getEmail());
        verify(userRepository, times(1)).existsByEmailAndIsEnabledFalse(any(String.class));
        verify(userRepository, times(1)).existsByEmailAndIsNonLockedFalse(any(String.class));
        verify(userMapper, never()).createRequestToUser(any(UserRegisterRequest.class));
        verify(userRepository, never()).saveAndFlush(any(User.class));
        verify(userMapper, never()).userToResponse(any(User.class));

        assertEquals(String.format("User with email: %s, already exists and is locked.", userRegisterRequest.getEmail()), thrownException.getMessage());
    }

    @Test
    void login_shouldReturnLoginResponse_onSuccessfulAuthenticationAndTokenSave() {

        LoginRequest loginRequest = createLoginRequest (EMAIL, PASSWORD);

        User existingUser = createUser(USER_ID, EMAIL, PASSWORD_HASH, "First Name", "Last Name", UserRole.CLIENT, true,true, Instant.now().minus(10L, ChronoUnit.DAYS), Instant.now().minus(10L, ChronoUnit.DAYS), null);

        UserDetails userDetails = mock(UserDetails.class);
        Authentication authentication = mock(Authentication.class);

        when(userDetails.getUsername()).thenReturn(EMAIL);
        when(authentication.getPrincipal()).thenReturn(userDetails);
        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class))).thenReturn(authentication);
        when(jwtService.generateAccessToken(EMAIL)).thenReturn(ACCESS_TOKEN);
        when(jwtService.generateRefreshToken(EMAIL)).thenReturn(REFRESH_TOKEN);
        when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.of(existingUser));
        when(userRepository.saveAndFlush(any(User.class))).thenAnswer(invocation -> {
            User logedInUser = invocation.getArgument(0);
            logedInUser.setRefreshToken(REFRESH_TOKEN);
            return logedInUser;
        });

        LoginResponse actualResponse = authService.login(loginRequest);

        verify(authenticationManager, times(1)).authenticate(any(UsernamePasswordAuthenticationToken.class));
        verify(jwtService, times(1)).generateAccessToken(EMAIL);
        verify(jwtService, times(1)).generateRefreshToken(EMAIL);
        verify(userRepository, times(1)).findByEmail(EMAIL);
        verify(userRepository, times(1)).saveAndFlush(existingUser);

        assertNotNull(actualResponse);
        assertEquals(ACCESS_TOKEN, actualResponse.getAccessToken());
        assertEquals(REFRESH_TOKEN, actualResponse.getRefreshToken());
        assertEquals(authentication, SecurityContextHolder.getContext().getAuthentication());
        assertEquals(REFRESH_TOKEN, existingUser.getRefreshToken());
    }

}