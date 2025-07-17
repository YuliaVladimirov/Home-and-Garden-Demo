package org.example.homeandgarden.security.service;

import org.example.homeandgarden.exception.DataAlreadyExistsException;
import org.example.homeandgarden.exception.DataNotFoundException;
import org.example.homeandgarden.security.config.JwtService;
import org.example.homeandgarden.security.dto.LoginRequest;
import org.example.homeandgarden.security.dto.LoginResponse;
import org.example.homeandgarden.security.dto.RefreshRequest;
import org.example.homeandgarden.security.dto.RefreshResponse;
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
    private final UserRole USER_ROLE_CLIENT = UserRole.CLIENT;
    private final String EMAIL = "test@example.com";

    private final String PASSWORD = "Raw Password";
    private final String PASSWORD_HASH = "Hashed Password";

    private final String ACCESS_TOKEN = "Access Token";
    private final String REFRESH_TOKEN = "Refresh Token";

    private final String INVALID_REFRESH_TOKEN = "Invalid Refresh Token";

    private final Instant TIMESTAMP_PAST = Instant.now().minus(10L, ChronoUnit.DAYS);
    private final Instant TIMESTAMP_NOW = Instant.now();

    @BeforeEach
    void setUp() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void registerUser_shouldRegisterUserSuccessfullyWhenEmailDoesNotExistAndPasswordsMatch() {

        UserRegisterRequest userRegisterRequest = UserRegisterRequest.builder()
                .email(EMAIL)
                .password(PASSWORD)
                .confirmPassword(PASSWORD)
                .firstName("First Name")
                .lastName("Last Name")
                .build();

        User userToRegister = User.builder()
                .userId(null)
                .email(userRegisterRequest.getEmail())
                .passwordHash(PASSWORD_HASH)
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

        UserRegisterRequest userRegisterRequest = UserRegisterRequest.builder()
                .email(EMAIL)
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
                .email(EMAIL)
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
    void registerUser_shouldThrowDataAlreadyExistsExceptionWhenUserExistsAndIsDisabled() {

        UserRegisterRequest userRegisterRequest = UserRegisterRequest.builder()
                .email(EMAIL)
                .password(PASSWORD)
                .confirmPassword(PASSWORD)
                .firstName("First Name")
                .lastName("Last Name")
                .build();

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

        UserRegisterRequest userRegisterRequest = UserRegisterRequest.builder()
                .email(EMAIL)
                .password(PASSWORD)
                .confirmPassword(PASSWORD)
                .firstName("First Name")
                .lastName("Last Name")
                .build();

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
    void login_shouldReturnLoginResponseOnSuccessfulAuthenticationAndTokenSave() {

        LoginRequest loginRequest = LoginRequest.builder()
                .email(EMAIL)
                .password(PASSWORD)
                .build();

        User existingUser = User.builder()
                .userId(USER_ID)
                .email(EMAIL)
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
        when(userDetails.getUsername()).thenReturn(EMAIL);
        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class))).thenReturn(authentication);
        when(jwtService.generateAccessToken(EMAIL)).thenReturn(ACCESS_TOKEN);
        when(jwtService.generateRefreshToken(EMAIL)).thenReturn(REFRESH_TOKEN);
        when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.of(existingUser));
        when(userRepository.saveAndFlush(any(User.class))).thenAnswer(invocation -> {
            User user = invocation.getArgument(0);
            user.setRefreshToken(REFRESH_TOKEN);
            return user;
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

    @Test
    void login_shouldThrowBadCredentialsExceptionOnAuthenticationFailure() {

        LoginRequest loginRequest = LoginRequest.builder()
                .email(EMAIL)
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

        assertEquals("Invalid email or password.", thrown.getMessage());
        assertNull(SecurityContextHolder.getContext().getAuthentication());
    }

    @Test
    void login_shouldThrowDataNotFoundExceptionWhenUserNotFoundAfterAuthentication() {

        LoginRequest loginRequest = LoginRequest.builder()
                .email(EMAIL)
                .password(PASSWORD)
                .build();

        UserDetails userDetails = mock(UserDetails.class);
        Authentication authentication = mock(Authentication.class);

        when(userDetails.getUsername()).thenReturn(EMAIL);
        when(authentication.getPrincipal()).thenReturn(userDetails);
        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenReturn(authentication);
        when(jwtService.generateAccessToken(EMAIL)).thenReturn(ACCESS_TOKEN);
        when(jwtService.generateRefreshToken(EMAIL)).thenReturn(REFRESH_TOKEN);
        when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.empty());

        DataNotFoundException thrown = assertThrows(DataNotFoundException.class, () ->
                authService.login(loginRequest));

        verify(authenticationManager, times(1)).authenticate(any(UsernamePasswordAuthenticationToken.class));
        verify(jwtService, times(1)).generateAccessToken(EMAIL);
        verify(jwtService, times(1)).generateRefreshToken(EMAIL);
        verify(userRepository, times(1)).findByEmail(EMAIL);
        verify(userRepository, never()).saveAndFlush(any(User.class));

        assertEquals(String.format("User with email: %s, was not found.", EMAIL), thrown.getMessage());
        assertNotNull(SecurityContextHolder.getContext().getAuthentication());
    }

    @Test
    void getNewAccessToken_shouldReturnRefreshResponseOnSuccessfulRefreshTokenValidation() {

        RefreshRequest refreshRequest = RefreshRequest.builder()
                .refreshToken(REFRESH_TOKEN)
                .build();

        User existingUser = User.builder()
                .userId(USER_ID)
                .email(EMAIL)
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
        when(jwtService.getUserEmailFromRefreshToken(refreshRequest.getRefreshToken())).thenReturn(EMAIL);
        when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.of(existingUser));
        when(jwtService.generateAccessToken(EMAIL)).thenReturn(ACCESS_TOKEN);

        RefreshResponse actualResponse = authService.getNewAccessToken(refreshRequest);

        verify(jwtService, times(1)).isRefreshTokenValid(refreshRequest.getRefreshToken());
        verify(jwtService, times(1)).getUserEmailFromRefreshToken(refreshRequest.getRefreshToken());
        verify(userRepository, times(1)).findByEmail(EMAIL);
        verify(userRepository, never()).saveAndFlush(any(User.class));
        verify(jwtService, times(1)).generateAccessToken(EMAIL);

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

        assertEquals("Refresh token is missing or empty. Please log in again.", thrown.getMessage());
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

        assertEquals("Refresh token is missing or empty. Please log in again.", thrown.getMessage());
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

        assertEquals("Invalid or expired refresh token. Please log in again.", thrown.getMessage());
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

        assertEquals("Token does not contain a user identifier. Please log in again.", thrown.getMessage());
    }

    @Test
    void getNewAccessToken_shouldThrowDataNotFoundExceptionWhenUserNotFoundInDatabase() {

        RefreshRequest refreshRequest = RefreshRequest.builder()
                .refreshToken(REFRESH_TOKEN)
                .build();

        when(jwtService.isRefreshTokenValid(refreshRequest.getRefreshToken())).thenReturn(true);
        when(jwtService.getUserEmailFromRefreshToken(refreshRequest.getRefreshToken())).thenReturn(EMAIL);
        when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.empty());

        DataNotFoundException thrown = assertThrows(DataNotFoundException.class, () ->
                authService.getNewAccessToken(refreshRequest));

        verify(jwtService, times(1)).isRefreshTokenValid(refreshRequest.getRefreshToken());
        verify(jwtService, times(1)).getUserEmailFromRefreshToken(REFRESH_TOKEN);
        verify(userRepository, times(1)).findByEmail(EMAIL);
        verify(userRepository, never()).saveAndFlush(any(User.class));
        verify(jwtService, never()).generateAccessToken(any(String.class));

        assertEquals("User associated with refresh token not found. Please log in again.", thrown.getMessage());
    }

    @Test
    void getNewAccessToken_shouldThrowBadCredentialsExceptionWhenRefreshTokenDoesNotMatchRefreshTokenInDatabase() {

        RefreshRequest refreshRequest = RefreshRequest.builder()
                .refreshToken(INVALID_REFRESH_TOKEN)
                .build();

        User existingUser = User.builder()
                .userId(USER_ID)
                .email(EMAIL)
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
        when(jwtService.getUserEmailFromRefreshToken(refreshRequest.getRefreshToken())).thenReturn(EMAIL);
        when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.of(existingUser));

        BadCredentialsException thrown = assertThrows(BadCredentialsException.class, () ->
                authService.getNewAccessToken(refreshRequest));

        verify(jwtService, times(1)).isRefreshTokenValid(refreshRequest.getRefreshToken());
        verify(jwtService, times(1)).getUserEmailFromRefreshToken(refreshRequest.getRefreshToken());
        verify(userRepository, times(1)).findByEmail(EMAIL);
        verify(userRepository, times(1)).saveAndFlush(existingUser);
        verify(jwtService, never()).generateAccessToken(EMAIL);

        assertEquals("Refresh token mismatch or reuse detected. Please log in again.", thrown.getMessage());
    }
}