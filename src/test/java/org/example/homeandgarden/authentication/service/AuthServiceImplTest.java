package org.example.homeandgarden.authentication.service;

import org.example.homeandgarden.authentication.dto.*;
import org.example.homeandgarden.email.service.EmailService;
import org.example.homeandgarden.exception.DataAlreadyExistsException;
import org.example.homeandgarden.exception.DataNotFoundException;
import org.example.homeandgarden.exception.UserDisabledException;
import org.example.homeandgarden.exception.UserLockedException;
import org.example.homeandgarden.security.config.JwtService;
import org.example.homeandgarden.shared.MessageResponse;
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
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceImplTest {

    @Value("${app.password-reset.base-url}")
    private String resetBaseUrl;

    @Mock
    private AuthenticationManager authenticationManager;

    @Mock
    private JwtService jwtService;

    @Mock
    private EmailService emailService;

    @Mock
    private UserRepository userRepository;

    @Mock
    private UserMapper userMapper;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private AuthServiceImpl authService;

    private static final UUID USER_ID = UUID.fromString("aebdd1a2-1bc2-4cc9-8496-674e4c7ee5f2");
    private static final UserRole USER_ROLE_CLIENT = UserRole.CLIENT;
    private static final String USER_EMAIL = "test@example.com";

    private static final String PASSWORD = "Raw Password";
    private static final String PASSWORD_HASH = "Hashed Password";
    private static final String NEW_PASSWORD = "New Password";
    private static final String NEW_PASSWORD_HASH = "New Hashed Password";

    private static final String ACCESS_TOKEN = "Access Token";
    private static final String REFRESH_TOKEN = "Refresh Token";
    private static final String PASSWORD_RESET_TOKEN = "Password Reset Token";

    private static final String INVALID_REFRESH_TOKEN = "Invalid Refresh Token";
    private static final String INVALID_PASSWORD_RESET_TOKEN = "Invalid Password Reset Token";

    private final Instant TIMESTAMP_NOW = Instant.now();
    private static final Instant TIMESTAMP_PAST = Instant.parse("2024-12-01T12:00:00Z");

    @BeforeEach
    void setUp() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void registerUser_shouldRegisterUserSuccessfullyWhenEmailDoesNotExistAndPasswordsMatch() {

        UserRegisterRequest userRegisterRequest = UserRegisterRequest.builder()
                .email(USER_EMAIL)
                .password(PASSWORD)
                .confirmPassword(PASSWORD)
                .firstName("First Name")
                .lastName("Last Name")
                .build();

        User userToRegister = User.builder()
                .userId(null)
                .email(userRegisterRequest.getEmail())
                .passwordHash(null)
                .firstName(userRegisterRequest.getFirstName())
                .lastName(userRegisterRequest.getLastName())
                .userRole(USER_ROLE_CLIENT)
                .isEnabled(true)
                .isNonLocked(true)
                .registeredAt(null)
                .updatedAt(null)
                .refreshToken(null)
                .build();

        User registeredUser = User.builder()
                .userId(USER_ID)
                .email(userToRegister.getEmail())
                .passwordHash(userToRegister.getPasswordHash())
                .firstName(userToRegister.getFirstName())
                .lastName(userToRegister.getLastName())
                .userRole(userToRegister.getUserRole())
                .isEnabled(userToRegister.getIsEnabled())
                .isNonLocked(userToRegister.getIsNonLocked())
                .registeredAt(TIMESTAMP_NOW)
                .updatedAt(TIMESTAMP_NOW)
                .refreshToken(userToRegister.getRefreshToken())
                .build();

        UserResponse userResponse = UserResponse.builder()
                .userId(registeredUser.getUserId())
                .email(registeredUser.getEmail())
                .firstName(registeredUser.getFirstName())
                .lastName(registeredUser.getLastName())
                .userRole(registeredUser.getUserRole())
                .registeredAt(registeredUser.getRegisteredAt())
                .updatedAt(registeredUser.getUpdatedAt())
                .build();

        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);

        when(userRepository.existsByEmail(userRegisterRequest.getEmail())).thenReturn(false);
        when(userMapper.createRequestToUser(userRegisterRequest)).thenReturn(userToRegister);
        userToRegister.setPasswordHash(passwordEncoder.encode(userRegisterRequest.getPassword()));
        when(userRepository.saveAndFlush(userToRegister)).thenReturn(registeredUser);
        when(userMapper.userToResponse(registeredUser)).thenReturn(userResponse);

        UserResponse actualResponse = authService.registerUser(userRegisterRequest);

        verify(userRepository, times(1)).existsByEmail(userRegisterRequest.getEmail());
        verify(userRepository, never()).existsByEmailAndIsEnabledFalse(any(String.class));
        verify(userRepository, never()).existsByEmailAndIsNonLockedFalse(any(String.class));
        verify(userMapper, times(1)).createRequestToUser(userRegisterRequest);

        verify(userRepository, times(1)).saveAndFlush(userCaptor.capture());
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

        UserRegisterRequest userRegisterRequest = UserRegisterRequest.builder()
                .email(USER_EMAIL)
                .password(PASSWORD)
                .confirmPassword("Wrong Password")
                .firstName("First Name")
                .lastName("Last Name")
                .build();

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

        UserRegisterRequest userRegisterRequest = UserRegisterRequest.builder()
                .email(USER_EMAIL)
                .password(PASSWORD)
                .confirmPassword(PASSWORD)
                .firstName("First Name")
                .lastName("Last Name")
                .build();

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
    void registerUser_shouldThrowUserDisabledExceptionWhenUserExistsAndIsDisabled() {

        UserRegisterRequest userRegisterRequest = UserRegisterRequest.builder()
                .email(USER_EMAIL)
                .password(PASSWORD)
                .confirmPassword(PASSWORD)
                .firstName("First Name")
                .lastName("Last Name")
                .build();

        when(userRepository.existsByEmail(userRegisterRequest.getEmail())).thenReturn(true);
        when(userRepository.existsByEmailAndIsEnabledFalse(userRegisterRequest.getEmail())).thenReturn(true);

        UserDisabledException thrownException = assertThrows(UserDisabledException.class, () -> authService.registerUser(userRegisterRequest));

        verify(userRepository, times(1)).existsByEmail(userRegisterRequest.getEmail());
        verify(userRepository, times(1)).existsByEmailAndIsEnabledFalse(any(String.class));
        verify(userRepository, never()).existsByEmailAndIsNonLockedFalse(any(String.class));
        verify(userMapper, never()).createRequestToUser(any(UserRegisterRequest.class));
        verify(userRepository, never()).saveAndFlush(any(User.class));
        verify(userMapper, never()).userToResponse(any(User.class));

        assertEquals(String.format("User with email: %s, is disabled.", userRegisterRequest.getEmail()), thrownException.getMessage());
    }

    @Test
    void registerUser_shouldThrowUserLockedExceptionWhenUserExistsAndIsLocked() {

        UserRegisterRequest userRegisterRequest = UserRegisterRequest.builder()
                .email(USER_EMAIL)
                .password(PASSWORD)
                .confirmPassword(PASSWORD)
                .firstName("First Name")
                .lastName("Last Name")
                .build();

        when(userRepository.existsByEmail(userRegisterRequest.getEmail())).thenReturn(true);
        when(userRepository.existsByEmailAndIsEnabledFalse(userRegisterRequest.getEmail())).thenReturn(false);
        when(userRepository.existsByEmailAndIsNonLockedFalse(userRegisterRequest.getEmail())).thenReturn(true);

        UserLockedException thrownException = assertThrows(UserLockedException.class, () -> authService.registerUser(userRegisterRequest));

        verify(userRepository, times(1)).existsByEmail(userRegisterRequest.getEmail());
        verify(userRepository, times(1)).existsByEmailAndIsEnabledFalse(any(String.class));
        verify(userRepository, times(1)).existsByEmailAndIsNonLockedFalse(any(String.class));
        verify(userMapper, never()).createRequestToUser(any(UserRegisterRequest.class));
        verify(userRepository, never()).saveAndFlush(any(User.class));
        verify(userMapper, never()).userToResponse(any(User.class));

        assertEquals(String.format("User with email: %s, is locked.", userRegisterRequest.getEmail()), thrownException.getMessage());
    }

    @Test
    void login_shouldReturnLoginResponseOnSuccessfulAuthenticationAndTokenSave() {

        LoginRequest loginRequest = LoginRequest.builder()
                .email(USER_EMAIL)
                .password(PASSWORD)
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
                .refreshToken(null)
                .build();

        UserDetails userDetails = mock(UserDetails.class);
        Authentication authentication = mock(Authentication.class);

        when(authentication.getPrincipal()).thenReturn(userDetails);
        when(userDetails.getUsername()).thenReturn(USER_EMAIL);
        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class))).thenReturn(authentication);
        when(jwtService.generateAccessToken(USER_EMAIL)).thenReturn(ACCESS_TOKEN);
        when(jwtService.generateRefreshToken(USER_EMAIL)).thenReturn(REFRESH_TOKEN);
        when(userRepository.findByEmail(USER_EMAIL)).thenReturn(Optional.of(existingUser));
        when(userRepository.saveAndFlush(any(User.class))).thenAnswer(invocation -> {
            User user = invocation.getArgument(0);
            user.setRefreshToken(REFRESH_TOKEN);
            return user;
        });

        LoginResponse actualResponse = authService.login(loginRequest);

        verify(authenticationManager, times(1)).authenticate(any(UsernamePasswordAuthenticationToken.class));
        verify(jwtService, times(1)).generateAccessToken(USER_EMAIL);
        verify(jwtService, times(1)).generateRefreshToken(USER_EMAIL);
        verify(userRepository, times(1)).findByEmail(USER_EMAIL);
        verify(userRepository, times(1)).saveAndFlush(existingUser);

        assertNotNull(actualResponse);
        assertEquals(ACCESS_TOKEN, actualResponse.getAccessToken());
        assertEquals(REFRESH_TOKEN, actualResponse.getRefreshToken());
        assertEquals(authentication, SecurityContextHolder.getContext().getAuthentication());
        assertEquals(REFRESH_TOKEN, existingUser.getRefreshToken());
    }

    @Test
    void login_shouldThrowBadCredentialsExceptionOnAuthenticationFailure() {

        LoginRequest loginRequest = LoginRequest.builder()
                .email(USER_EMAIL)
                .password(PASSWORD)
                .build();

        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenThrow(new BadCredentialsException("Invalid email or password provided during authentication."));

        BadCredentialsException thrown = assertThrows(BadCredentialsException.class, () ->
                authService.login(loginRequest));

        verify(authenticationManager, times(1)).authenticate(any(UsernamePasswordAuthenticationToken.class));
        verify(jwtService, never()).generateAccessToken(anyString());
        verify(jwtService, never()).generateRefreshToken(anyString());
        verify(userRepository, never()).findByEmail(anyString());
        verify(userRepository, never()).saveAndFlush(any(User.class));

        assertEquals("Invalid email or password. Please log in again.", thrown.getMessage());
        assertNull(SecurityContextHolder.getContext().getAuthentication());
    }

    @Test
    void login_shouldThrowDataNotFoundExceptionWhenUserNotFoundAfterAuthentication() {

        LoginRequest loginRequest = LoginRequest.builder()
                .email(USER_EMAIL)
                .password(PASSWORD)
                .build();

        UserDetails userDetails = mock(UserDetails.class);
        Authentication authentication = mock(Authentication.class);

        when(userDetails.getUsername()).thenReturn(USER_EMAIL);
        when(authentication.getPrincipal()).thenReturn(userDetails);
        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenReturn(authentication);
        when(jwtService.generateAccessToken(USER_EMAIL)).thenReturn(ACCESS_TOKEN);
        when(jwtService.generateRefreshToken(USER_EMAIL)).thenReturn(REFRESH_TOKEN);
        when(userRepository.findByEmail(USER_EMAIL)).thenReturn(Optional.empty());

        DataNotFoundException thrown = assertThrows(DataNotFoundException.class, () ->
                authService.login(loginRequest));

        verify(authenticationManager, times(1)).authenticate(any(UsernamePasswordAuthenticationToken.class));
        verify(jwtService, times(1)).generateAccessToken(USER_EMAIL);
        verify(jwtService, times(1)).generateRefreshToken(USER_EMAIL);
        verify(userRepository, times(1)).findByEmail(USER_EMAIL);
        verify(userRepository, never()).saveAndFlush(any(User.class));

        assertEquals(String.format("User with email: %s, was not found.", USER_EMAIL), thrown.getMessage());
        assertNotNull(SecurityContextHolder.getContext().getAuthentication());
    }

    @Test
    void getNewAccessToken_shouldReturnRefreshResponseOnSuccessfulRefreshTokenValidation() {

        RefreshRequest refreshRequest = RefreshRequest.builder()
                .refreshToken(REFRESH_TOKEN)
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
                .refreshToken(REFRESH_TOKEN)
                .build();

        when(jwtService.isRefreshTokenValid(refreshRequest.getRefreshToken())).thenReturn(true);
        when(jwtService.getUserEmailFromRefreshToken(refreshRequest.getRefreshToken())).thenReturn(USER_EMAIL);
        when(userRepository.findByEmail(USER_EMAIL)).thenReturn(Optional.of(existingUser));
        when(jwtService.generateAccessToken(USER_EMAIL)).thenReturn(ACCESS_TOKEN);

        RefreshResponse actualResponse = authService.getNewAccessToken(refreshRequest);

        verify(jwtService, times(1)).isRefreshTokenValid(refreshRequest.getRefreshToken());
        verify(jwtService, times(1)).getUserEmailFromRefreshToken(refreshRequest.getRefreshToken());
        verify(userRepository, times(1)).findByEmail(USER_EMAIL);
        verify(userRepository, never()).saveAndFlush(any(User.class));
        verify(jwtService, times(1)).generateAccessToken(USER_EMAIL);

        assertNotNull(actualResponse);
        assertEquals(ACCESS_TOKEN, actualResponse.getAccessToken());
    }


    @Test
    void getNewAccessToken_shouldThrowBadCredentialsExceptionWhenRefreshTokenMissing() {

        RefreshRequest refreshRequest = RefreshRequest.builder()
                .refreshToken(null)
                .build();

        BadCredentialsException thrown = assertThrows(BadCredentialsException.class, () ->
                authService.getNewAccessToken(refreshRequest));

        verify(jwtService, never()).isRefreshTokenValid(any(String.class));
        verify(jwtService, never()).getUserEmailFromRefreshToken(any(String.class));
        verify(userRepository, never()).findByEmail(any(String.class));
        verify(userRepository, never()).saveAndFlush(any(User.class));
        verify(jwtService, never()).generateAccessToken(any(String.class));

        assertEquals("Refresh token is missing or empty. Please log in.", thrown.getMessage());
    }

    @Test
    void getNewAccessToken_shouldThrowBadCredentialsExceptionWhenRefreshTokenEmpty() {

        RefreshRequest refreshRequest = RefreshRequest.builder()
                .refreshToken("")
                .build();

        BadCredentialsException thrown = assertThrows(BadCredentialsException.class, () ->
                authService.getNewAccessToken(refreshRequest));

        verify(jwtService, never()).isRefreshTokenValid(any(String.class));
        verify(jwtService, never()).getUserEmailFromRefreshToken(any(String.class));
        verify(userRepository, never()).findByEmail(any(String.class));
        verify(userRepository, never()).saveAndFlush(any(User.class));
        verify(jwtService, never()).generateAccessToken(any(String.class));

        assertEquals("Refresh token is missing or empty. Please log in.", thrown.getMessage());
    }

    @Test
    void getNewAccessToken_shouldThrowBadCredentialsExceptionOnRefreshTokenValidationFailure() {

        RefreshRequest refreshRequest = RefreshRequest.builder()
                .refreshToken(INVALID_REFRESH_TOKEN)
                .build();

        when(jwtService.isRefreshTokenValid(refreshRequest.getRefreshToken())).thenReturn(false);

        BadCredentialsException thrown = assertThrows(BadCredentialsException.class, () ->
                authService.getNewAccessToken(refreshRequest));

        verify(jwtService, times(1)).isRefreshTokenValid(refreshRequest.getRefreshToken());
        verify(jwtService, never()).getUserEmailFromRefreshToken(any(String.class));
        verify(userRepository, never()).findByEmail(any(String.class));
        verify(userRepository, never()).saveAndFlush(any(User.class));
        verify(jwtService, never()).generateAccessToken(any(String.class));

        assertEquals("Invalid or expired refresh token. Please log in.", thrown.getMessage());
    }

    @Test
    void getNewAccessToken_shouldThrowBadCredentialsExceptionWhenRefreshTokenDoesNotContainUserEmail() {

        RefreshRequest refreshRequest = RefreshRequest.builder()
                .refreshToken(INVALID_REFRESH_TOKEN)
                .build();

        when(jwtService.isRefreshTokenValid(refreshRequest.getRefreshToken())).thenReturn(true);
        when(jwtService.getUserEmailFromRefreshToken(refreshRequest.getRefreshToken())).thenReturn(null);

        BadCredentialsException thrown = assertThrows(BadCredentialsException.class, () ->
                authService.getNewAccessToken(refreshRequest));

        verify(jwtService, times(1)).isRefreshTokenValid(refreshRequest.getRefreshToken());
        verify(jwtService, times(1)).getUserEmailFromRefreshToken(INVALID_REFRESH_TOKEN);
        verify(userRepository, never()).findByEmail(any(String.class));
        verify(userRepository, never()).saveAndFlush(any(User.class));
        verify(jwtService, never()).generateAccessToken(any(String.class));

        assertEquals("Token does not contain a user identifier. Please log in.", thrown.getMessage());
    }

    @Test
    void getNewAccessToken_shouldThrowDataNotFoundExceptionWhenUserNotFoundInDatabase() {

        RefreshRequest refreshRequest = RefreshRequest.builder()
                .refreshToken(REFRESH_TOKEN)
                .build();

        when(jwtService.isRefreshTokenValid(refreshRequest.getRefreshToken())).thenReturn(true);
        when(jwtService.getUserEmailFromRefreshToken(refreshRequest.getRefreshToken())).thenReturn(USER_EMAIL);
        when(userRepository.findByEmail(USER_EMAIL)).thenReturn(Optional.empty());

        DataNotFoundException thrown = assertThrows(DataNotFoundException.class, () ->
                authService.getNewAccessToken(refreshRequest));

        verify(jwtService, times(1)).isRefreshTokenValid(refreshRequest.getRefreshToken());
        verify(jwtService, times(1)).getUserEmailFromRefreshToken(REFRESH_TOKEN);
        verify(userRepository, times(1)).findByEmail(USER_EMAIL);
        verify(userRepository, never()).saveAndFlush(any(User.class));
        verify(jwtService, never()).generateAccessToken(any(String.class));

        assertEquals(String.format("User with email: %s, associated with refresh token, not found. Please log in.", USER_EMAIL), thrown.getMessage());
    }

    @Test
    void getNewAccessToken_shouldThrowUserDisabledExceptionWhenUserIsUnregistered() {

        RefreshRequest refreshRequest = RefreshRequest.builder()
                .refreshToken(REFRESH_TOKEN)
                .build();

        User existingUser = User.builder()
                .userId(USER_ID)
                .email(USER_EMAIL)
                .passwordHash(PASSWORD_HASH)
                .firstName("First Name")
                .lastName("Last Name")
                .userRole(USER_ROLE_CLIENT)
                .isEnabled(false)
                .isNonLocked(true)
                .registeredAt(TIMESTAMP_PAST)
                .updatedAt(TIMESTAMP_PAST)
                .refreshToken(REFRESH_TOKEN)
                .build();

        when(jwtService.isRefreshTokenValid(refreshRequest.getRefreshToken())).thenReturn(true);
        when(jwtService.getUserEmailFromRefreshToken(refreshRequest.getRefreshToken())).thenReturn(USER_EMAIL);
        when(userRepository.findByEmail(USER_EMAIL)).thenReturn(Optional.of(existingUser));

        UserDisabledException thrown = assertThrows(UserDisabledException.class, () ->
                authService.getNewAccessToken(refreshRequest));

        verify(jwtService, times(1)).isRefreshTokenValid(refreshRequest.getRefreshToken());
        verify(jwtService, times(1)).getUserEmailFromRefreshToken(REFRESH_TOKEN);
        verify(userRepository, times(1)).findByEmail(USER_EMAIL);
        verify(userRepository, never()).saveAndFlush(any(User.class));
        verify(jwtService, never()).generateAccessToken(any(String.class));

        assertEquals(String.format("User with email: %s, is disabled.", USER_EMAIL), thrown.getMessage());
    }

    @Test
    void getNewAccessToken_shouldThrowUserLockedExceptionWhenUserIsLocked() {

        RefreshRequest refreshRequest = RefreshRequest.builder()
                .refreshToken(REFRESH_TOKEN)
                .build();

        User existingUser = User.builder()
                .userId(USER_ID)
                .email(USER_EMAIL)
                .passwordHash(PASSWORD_HASH)
                .firstName("First Name")
                .lastName("Last Name")
                .userRole(USER_ROLE_CLIENT)
                .isEnabled(true)
                .isNonLocked(false)
                .registeredAt(TIMESTAMP_PAST)
                .updatedAt(TIMESTAMP_PAST)
                .refreshToken(REFRESH_TOKEN)
                .build();

        when(jwtService.isRefreshTokenValid(refreshRequest.getRefreshToken())).thenReturn(true);
        when(jwtService.getUserEmailFromRefreshToken(refreshRequest.getRefreshToken())).thenReturn(USER_EMAIL);
        when(userRepository.findByEmail(USER_EMAIL)).thenReturn(Optional.of(existingUser));

        UserLockedException thrown = assertThrows(UserLockedException.class, () ->
                authService.getNewAccessToken(refreshRequest));

        verify(jwtService, times(1)).isRefreshTokenValid(refreshRequest.getRefreshToken());
        verify(jwtService, times(1)).getUserEmailFromRefreshToken(REFRESH_TOKEN);
        verify(userRepository, times(1)).findByEmail(USER_EMAIL);
        verify(userRepository, never()).saveAndFlush(any(User.class));
        verify(jwtService, never()).generateAccessToken(any(String.class));

        assertEquals(String.format("User with email: %s, is locked.", USER_EMAIL), thrown.getMessage());
    }

    @Test
    void getNewAccessToken_shouldThrowBadCredentialsExceptionWhenRefreshTokenDoesNotMatchRefreshTokenInDatabase() {

        RefreshRequest refreshRequest = RefreshRequest.builder()
                .refreshToken(INVALID_REFRESH_TOKEN)
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
                .refreshToken(REFRESH_TOKEN)
                .build();

        when(jwtService.isRefreshTokenValid(refreshRequest.getRefreshToken())).thenReturn(true);
        when(jwtService.getUserEmailFromRefreshToken(refreshRequest.getRefreshToken())).thenReturn(USER_EMAIL);
        when(userRepository.findByEmail(USER_EMAIL)).thenReturn(Optional.of(existingUser));

        BadCredentialsException thrown = assertThrows(BadCredentialsException.class, () ->
                authService.getNewAccessToken(refreshRequest));

        verify(jwtService, times(1)).isRefreshTokenValid(refreshRequest.getRefreshToken());
        verify(jwtService, times(1)).getUserEmailFromRefreshToken(refreshRequest.getRefreshToken());
        verify(userRepository, times(1)).findByEmail(USER_EMAIL);
        verify(userRepository, times(1)).saveAndFlush(existingUser);
        verify(jwtService, never()).generateAccessToken(USER_EMAIL);

        assertEquals("Refresh token mismatch or reuse detected. Please log in.", thrown.getMessage());
    }

    @Test
    void forgotPassword_shouldGenerateTokenAndSendEmailAndReturnSuccessMessageWhenUserExistsAndIsEnabledAndIsNotLocked() {

        ForgotPasswordRequest request = ForgotPasswordRequest.builder()
                .email(USER_EMAIL)
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
                .refreshToken(REFRESH_TOKEN)
                .build();

        when(userRepository.findByEmail(request.getEmail())).thenReturn(Optional.of(existingUser));
        when(jwtService.generatePasswordResetToken(existingUser.getEmail())).thenReturn(PASSWORD_RESET_TOKEN);
        existingUser.setPasswordResetToken(PASSWORD_RESET_TOKEN);

        MessageResponse messageResponse = authService.forgotPassword(request);

        verify(userRepository, times(1)).findByEmail(request.getEmail());
        verify(jwtService, times(1)).generatePasswordResetToken(existingUser.getEmail());
        verify(userRepository, times(1)).saveAndFlush(existingUser);

        String expectedLink = resetBaseUrl + "?token=" + PASSWORD_RESET_TOKEN;
        verify(emailService).sendPasswordResetEmail(USER_EMAIL, "Password Reset Request", expectedLink);

        assertEquals(existingUser.getPasswordResetToken(), PASSWORD_RESET_TOKEN);
        assertNotNull(messageResponse);
        assertEquals("Password reset link sent to user's email.", messageResponse.getMessage());
    }

    @Test
    void forgotPassword_shouldThrowDataNotFoundExceptionWhenUserDoesNotExist() {

        ForgotPasswordRequest request = ForgotPasswordRequest.builder()
                .email(USER_EMAIL)
                .build();

        when(userRepository.findByEmail(request.getEmail())).thenReturn(Optional.empty());

        DataNotFoundException thrown = assertThrows(DataNotFoundException.class, () -> authService.forgotPassword(request));

        verify(userRepository, times(1)).findByEmail(request.getEmail());
        verify(jwtService, never()).generatePasswordResetToken(any(String.class));
        verify(userRepository, never()).saveAndFlush(any(User.class));
        verify(emailService, never()).sendPasswordResetEmail(any(String.class), any(String.class), any(String.class));

        assertEquals(String.format("User with email: %s, was not found.", USER_EMAIL), thrown.getMessage());
    }

    @Test
    void forgotPassword__shouldThrowUserDisabledExceptionWhenUserIsDisabled() {

        ForgotPasswordRequest request = ForgotPasswordRequest.builder()
                .email(USER_EMAIL)
                .build();

        User disabledUser = User.builder()
                .userId(USER_ID)
                .email(USER_EMAIL)
                .passwordHash(PASSWORD_HASH)
                .firstName("First Name")
                .lastName("Last Name")
                .userRole(USER_ROLE_CLIENT)
                .isEnabled(false)
                .isNonLocked(true)
                .registeredAt(TIMESTAMP_PAST)
                .updatedAt(TIMESTAMP_PAST)
                .refreshToken(REFRESH_TOKEN)
                .build();

        when(userRepository.findByEmail(request.getEmail())).thenReturn(Optional.of(disabledUser));

        UserDisabledException thrown = assertThrows(UserDisabledException.class, () -> authService.forgotPassword(request));

        verify(userRepository, times(1)).findByEmail(request.getEmail());
        verify(jwtService, never()).generatePasswordResetToken(any(String.class));
        verify(userRepository, never()).saveAndFlush(any(User.class));
        verify(emailService, never()).sendPasswordResetEmail(any(String.class), any(String.class), any(String.class));

        assertEquals(String.format("User with email: %s, is disabled.", USER_EMAIL), thrown.getMessage());
    }

    @Test
    void forgotPassword_shouldThrowUserLockedExceptionWhenUserIsLocked() {

        ForgotPasswordRequest request = ForgotPasswordRequest.builder()
                .email(USER_EMAIL)
                .build();

        User lockedUser = User.builder()
                .userId(USER_ID)
                .email(USER_EMAIL)
                .passwordHash(PASSWORD_HASH)
                .firstName("First Name")
                .lastName("Last Name")
                .userRole(USER_ROLE_CLIENT)
                .isEnabled(true)
                .isNonLocked(false)
                .registeredAt(TIMESTAMP_PAST)
                .updatedAt(TIMESTAMP_PAST)
                .refreshToken(REFRESH_TOKEN)
                .build();

        when(userRepository.findByEmail(request.getEmail())).thenReturn(Optional.of(lockedUser));

        UserLockedException thrown = assertThrows(UserLockedException.class, () -> authService.forgotPassword(request));

        verify(userRepository, times(1)).findByEmail(request.getEmail());
        verify(jwtService, never()).generatePasswordResetToken(any(String.class));
        verify(userRepository, never()).saveAndFlush(any(User.class));
        verify(emailService, never()).sendPasswordResetEmail(any(String.class), any(String.class), any(String.class));

        assertEquals(String.format("User with email: %s, is locked.", USER_EMAIL), thrown.getMessage());
    }

    @Test
    void resetPassword_shouldUpdatePasswordAndReturnSuccessMessageWhenRequestIsValid() {

        PasswordResetRequest passwordResetRequest = PasswordResetRequest.builder()
                .passwordResetToken(PASSWORD_RESET_TOKEN)
                .newPassword(NEW_PASSWORD)
                .confirmPassword(NEW_PASSWORD)
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
                .refreshToken(REFRESH_TOKEN)
                .passwordResetToken(PASSWORD_RESET_TOKEN)
                .build();

        when(jwtService.isPasswordResetTokenValid(PASSWORD_RESET_TOKEN)).thenReturn(true);
        when(jwtService.getUserEmailFromPasswordResetToken(PASSWORD_RESET_TOKEN)).thenReturn(USER_EMAIL);
        when(userRepository.findByEmail(USER_EMAIL)).thenReturn(Optional.of(existingUser));

        when(passwordEncoder.encode(NEW_PASSWORD)).thenReturn(NEW_PASSWORD_HASH);

        MessageResponse messageResponse = authService.resetPassword(passwordResetRequest);

        verify(jwtService, times(1)).isPasswordResetTokenValid(PASSWORD_RESET_TOKEN);
        verify(jwtService, times(1)).getUserEmailFromPasswordResetToken(PASSWORD_RESET_TOKEN);
        verify(userRepository, times(1)).findByEmail(USER_EMAIL);
        verify(userRepository, times(1)).saveAndFlush(existingUser);
        verify(passwordEncoder, times(1)).encode(NEW_PASSWORD);

        assertNotNull(messageResponse);
        assertEquals("Password has been successfully reset.", messageResponse.getMessage());
        assertEquals(existingUser.getPasswordHash(), NEW_PASSWORD_HASH);
        assertNull(existingUser.getPasswordResetToken());
    }

    @Test
    void resetPassword_shouldThrowBadCredentialsExceptionWhenPasswordsDoNotMatch() {

        PasswordResetRequest mismatchedRequest = PasswordResetRequest.builder()
                .passwordResetToken(PASSWORD_RESET_TOKEN)
                .newPassword(NEW_PASSWORD)
                .confirmPassword("Different Password")
                .build();

        BadCredentialsException thrown = assertThrows(BadCredentialsException.class, () ->authService.resetPassword(mismatchedRequest));

        verify(jwtService, never()).isPasswordResetTokenValid(any(String.class));
        verify(jwtService, never()).getUserEmailFromPasswordResetToken(any(String.class));
        verify(userRepository, never()).findByEmail(any(String.class));
        verify(userRepository, never()).saveAndFlush(any(User.class));
        verify(passwordEncoder, never()).encode(any(String.class));

        assertEquals("Password doesn't match the CONFIRM PASSWORD field. Please try resetting your password again", thrown.getMessage());
    }

    @Test
    void resetPassword_shouldThrowBadCredentialsExceptionWhenTokenIsMissing() {

        PasswordResetRequest nullTokenRequest = PasswordResetRequest.builder()
                .passwordResetToken(null)
                .newPassword(NEW_PASSWORD)
                .confirmPassword(NEW_PASSWORD)
                .build();

        BadCredentialsException thrown = assertThrows(BadCredentialsException.class, () -> authService.resetPassword(nullTokenRequest));

        verify(jwtService, never()).isPasswordResetTokenValid(any(String.class));
        verify(jwtService, never()).getUserEmailFromPasswordResetToken(any(String.class));
        verify(userRepository, never()).findByEmail(any(String.class));
        verify(userRepository, never()).saveAndFlush(any(User.class));
        verify(passwordEncoder, never()).encode(any(String.class));

        assertEquals("Password reset token is missing or empty. Please try resetting your password again.", thrown.getMessage());
    }

    @Test
    void resetPassword_shouldThrowBadCredentialsExceptionWhenTokenIsBlank() {

        PasswordResetRequest blankTokenRequest = PasswordResetRequest.builder()
                .passwordResetToken("")
                .newPassword(NEW_PASSWORD)
                .confirmPassword(NEW_PASSWORD)
                .build();

        BadCredentialsException thrown = assertThrows(BadCredentialsException.class, () -> authService.resetPassword(blankTokenRequest));

        verify(jwtService, never()).isPasswordResetTokenValid(any(String.class));
        verify(jwtService, never()).getUserEmailFromPasswordResetToken(any(String.class));
        verify(userRepository, never()).findByEmail(any(String.class));
        verify(userRepository, never()).saveAndFlush(any(User.class));
        verify(passwordEncoder, never()).encode(any(String.class));

        assertEquals("Password reset token is missing or empty. Please try resetting your password again.", thrown.getMessage());
    }

    @Test
    void resetPassword_shouldThrowBadCredentialsExceptionWhenTokenIsInvalidOrExpired() {

        PasswordResetRequest resetRequest = PasswordResetRequest.builder()
                .passwordResetToken(INVALID_PASSWORD_RESET_TOKEN)
                .newPassword(NEW_PASSWORD)
                .confirmPassword(NEW_PASSWORD)
                .build();

        when(jwtService.isPasswordResetTokenValid(resetRequest.getPasswordResetToken())).thenReturn(false);

        BadCredentialsException thrown = assertThrows(BadCredentialsException.class, () -> authService.resetPassword(resetRequest));

        verify(jwtService, times(1)).isPasswordResetTokenValid(resetRequest.getPasswordResetToken());
        verify(jwtService, times(1)).isPasswordResetTokenValid(resetRequest.getPasswordResetToken());
        verify(jwtService, never()).getUserEmailFromPasswordResetToken(any(String.class));
        verify(userRepository, never()).findByEmail(any(String.class));
        verify(userRepository, never()).saveAndFlush(any(User.class));
        verify(passwordEncoder, never()).encode(any(String.class));

        assertEquals("Invalid or expired password reset token. Please try resetting your password again.", thrown.getMessage());
    }

    @Test
    void resetPassword_shouldThrowBadCredentialsExceptionWhenTokenHasNoEmail() {

        PasswordResetRequest resetRequest = PasswordResetRequest.builder()
                .passwordResetToken(PASSWORD_RESET_TOKEN)
                .newPassword(NEW_PASSWORD)
                .confirmPassword(NEW_PASSWORD)
                .build();

        when(jwtService.isPasswordResetTokenValid(resetRequest.getPasswordResetToken())).thenReturn(true);
        when(jwtService.getUserEmailFromPasswordResetToken(resetRequest.getPasswordResetToken())).thenReturn(null);

        BadCredentialsException thrown = assertThrows(BadCredentialsException.class, () -> authService.resetPassword(resetRequest));

        verify(jwtService, times(1)).isPasswordResetTokenValid(resetRequest.getPasswordResetToken());
        verify(jwtService, times(1)).isPasswordResetTokenValid(resetRequest.getPasswordResetToken());
        verify(jwtService, times(1)).getUserEmailFromPasswordResetToken(resetRequest.getPasswordResetToken());
        verify(userRepository, never()).findByEmail(any(String.class));
        verify(userRepository, never()).saveAndFlush(any(User.class));
        verify(passwordEncoder, never()).encode(any(String.class));

        assertEquals("Token does not contain a user identifier. Please try resetting your password again.", thrown.getMessage());
    }

    @Test
    void resetPassword_shouldThrowDataNotFoundExceptionWhenUserDoesNotExist() {

        PasswordResetRequest resetRequest = PasswordResetRequest.builder()
                .passwordResetToken(PASSWORD_RESET_TOKEN)
                .newPassword(NEW_PASSWORD)
                .confirmPassword(NEW_PASSWORD)
                .build();

        when(jwtService.isPasswordResetTokenValid(resetRequest.getPasswordResetToken())).thenReturn(true);
        when(jwtService.getUserEmailFromPasswordResetToken(resetRequest.getPasswordResetToken())).thenReturn(USER_EMAIL);
        when(userRepository.findByEmail(USER_EMAIL)).thenReturn(Optional.empty());

        DataNotFoundException thrown = assertThrows(DataNotFoundException.class, () -> authService.resetPassword(resetRequest));

        verify(jwtService, times(1)).isPasswordResetTokenValid(resetRequest.getPasswordResetToken());
        verify(jwtService, times(1)).isPasswordResetTokenValid(resetRequest.getPasswordResetToken());
        verify(jwtService, times(1)).getUserEmailFromPasswordResetToken(resetRequest.getPasswordResetToken());
        verify(userRepository, times(1)).findByEmail(USER_EMAIL);
        verify(userRepository, never()).saveAndFlush(any(User.class));
        verify(passwordEncoder, never()).encode(any(String.class));

        assertEquals(String.format("User with email: %s, associated with password reset token, not found.", USER_EMAIL), thrown.getMessage());
    }

    @Test
    void resetPassword_shouldThrowUserDisabledExceptionWhenUserIsDisabled() {

        PasswordResetRequest resetRequest = PasswordResetRequest.builder()
                .passwordResetToken(PASSWORD_RESET_TOKEN)
                .newPassword(NEW_PASSWORD)
                .confirmPassword(NEW_PASSWORD)
                .build();

        User disabledUser = User.builder()
                .userId(USER_ID)
                .email(USER_EMAIL)
                .passwordHash(PASSWORD_HASH)
                .firstName("First Name")
                .lastName("Last Name")
                .userRole(USER_ROLE_CLIENT)
                .isEnabled(false)
                .isNonLocked(true)
                .registeredAt(TIMESTAMP_PAST)
                .updatedAt(TIMESTAMP_PAST)
                .refreshToken(REFRESH_TOKEN)
                .passwordResetToken(PASSWORD_RESET_TOKEN)
                .build();

        when(jwtService.isPasswordResetTokenValid(resetRequest.getPasswordResetToken())).thenReturn(true);
        when(jwtService.getUserEmailFromPasswordResetToken(resetRequest.getPasswordResetToken())).thenReturn(USER_EMAIL);
        when(userRepository.findByEmail(USER_EMAIL)).thenReturn(Optional.of(disabledUser));

        UserDisabledException thrown = assertThrows(UserDisabledException.class, () -> authService.resetPassword(resetRequest));

        verify(jwtService, times(1)).isPasswordResetTokenValid(resetRequest.getPasswordResetToken());
        verify(jwtService, times(1)).isPasswordResetTokenValid(resetRequest.getPasswordResetToken());
        verify(jwtService, times(1)).getUserEmailFromPasswordResetToken(resetRequest.getPasswordResetToken());
        verify(userRepository, times(1)).findByEmail(USER_EMAIL);
        verify(userRepository, never()).saveAndFlush(any(User.class));
        verify(passwordEncoder, never()).encode(any(String.class));

        assertEquals(String.format("User with email: %s, is disabled.", USER_EMAIL), thrown.getMessage());
    }

    @Test
    void resetPassword_shouldThrowUserLockedExceptionWhenUserIsLocked() {

        PasswordResetRequest resetRequest = PasswordResetRequest.builder()
                .passwordResetToken(PASSWORD_RESET_TOKEN)
                .newPassword(NEW_PASSWORD)
                .confirmPassword(NEW_PASSWORD)
                .build();

        User lockedUser = User.builder()
                .userId(USER_ID)
                .email(USER_EMAIL)
                .passwordHash(PASSWORD_HASH)
                .firstName("First Name")
                .lastName("Last Name")
                .userRole(USER_ROLE_CLIENT)
                .isEnabled(true)
                .isNonLocked(false)
                .registeredAt(TIMESTAMP_PAST)
                .updatedAt(TIMESTAMP_PAST)
                .refreshToken(REFRESH_TOKEN)
                .passwordResetToken(PASSWORD_RESET_TOKEN)
                .build();

        when(jwtService.isPasswordResetTokenValid(resetRequest.getPasswordResetToken())).thenReturn(true);
        when(jwtService.getUserEmailFromPasswordResetToken(resetRequest.getPasswordResetToken())).thenReturn(USER_EMAIL);
        when(userRepository.findByEmail(USER_EMAIL)).thenReturn(Optional.of(lockedUser));

        UserLockedException thrown = assertThrows(UserLockedException.class, () -> authService.resetPassword(resetRequest));

        verify(jwtService, times(1)).isPasswordResetTokenValid(resetRequest.getPasswordResetToken());
        verify(jwtService, times(1)).isPasswordResetTokenValid(resetRequest.getPasswordResetToken());
        verify(jwtService, times(1)).getUserEmailFromPasswordResetToken(resetRequest.getPasswordResetToken());
        verify(userRepository, times(1)).findByEmail(USER_EMAIL);
        verify(userRepository, never()).saveAndFlush(any(User.class));
        verify(passwordEncoder, never()).encode(any(String.class));

        assertEquals(String.format("User with email: %s, is locked.", USER_EMAIL), thrown.getMessage());
    }

    @Test
    void resetPassword_shouldThrowBadCredentialsExceptionAndClearUserTokenWhenTokenMismatch() {

        PasswordResetRequest resetRequest = PasswordResetRequest.builder()
                .passwordResetToken(PASSWORD_RESET_TOKEN)
                .newPassword(NEW_PASSWORD)
                .confirmPassword(NEW_PASSWORD)
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
                .refreshToken(REFRESH_TOKEN)
                .passwordResetToken("Other Token")
                .build();

        when(jwtService.isPasswordResetTokenValid(resetRequest.getPasswordResetToken())).thenReturn(true);
        when(jwtService.getUserEmailFromPasswordResetToken(resetRequest.getPasswordResetToken())).thenReturn(USER_EMAIL);
        when(userRepository.findByEmail(USER_EMAIL)).thenReturn(Optional.of(existingUser));
        existingUser.setPasswordResetToken(null);

        BadCredentialsException thrown = assertThrows(BadCredentialsException.class, () -> authService.resetPassword(resetRequest));

        verify(jwtService, times(1)).isPasswordResetTokenValid(resetRequest.getPasswordResetToken());
        verify(jwtService, times(1)).isPasswordResetTokenValid(resetRequest.getPasswordResetToken());
        verify(jwtService, times(1)).getUserEmailFromPasswordResetToken(resetRequest.getPasswordResetToken());
        verify(userRepository, times(1)).findByEmail(USER_EMAIL);
        verify(userRepository, times(1)).saveAndFlush(existingUser);
        verify(passwordEncoder, never()).encode(any(String.class));

        assertNull(existingUser.getPasswordResetToken());
        assertEquals("Password reset token mismatch or reuse detected. Please try resetting your password again.", thrown.getMessage());
    }
}