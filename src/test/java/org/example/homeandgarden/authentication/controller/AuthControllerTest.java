package org.example.homeandgarden.authentication.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.example.homeandgarden.authentication.dto.*;
import org.example.homeandgarden.authentication.service.AuthServiceImpl;
import org.example.homeandgarden.shared.MessageResponse;
import org.example.homeandgarden.user.dto.UserRegisterRequest;
import org.example.homeandgarden.user.dto.UserResponse;
import org.example.homeandgarden.user.entity.enums.UserRole;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.UUID;

import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Import(AuthControllerTest.TestConfig.class)
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @TestConfiguration
    static class TestConfig {

        @Bean
        @Primary
        public AuthServiceImpl authService() {
            return mock(AuthServiceImpl.class);
        }

    }

    @Autowired
    private AuthServiceImpl authService;

    @AfterEach
    void resetMocks() {
        reset(authService);
    }


// 🌐 Public access endpoints — no authentication required (accessible to all users)

    @Test
    void registerUser_shouldReturnCreatedUser_whenValidRequest() throws Exception {

        UserRegisterRequest registerRequest = UserRegisterRequest.builder()
                .email("test.user@example.com")
                .password("Password123!")
                .confirmPassword("Password123!")
                .firstName("First Name")
                .lastName("Last Name")
                .build();
        
        UserResponse expectedResponse = UserResponse.builder()
                .userId(UUID.randomUUID())
                .email(registerRequest.getEmail())
                .firstName(registerRequest.getFirstName())
                .lastName(registerRequest.getLastName())
                .userRole(UserRole.CLIENT)
                .isEnabled(true)
                .isNonLocked(true)
                .registeredAt(Instant.now())
                .updatedAt(Instant.now())
                .build();

        when(authService.registerUser(eq(registerRequest))).thenReturn(expectedResponse);

        mockMvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(registerRequest))
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.userId").exists())
                .andExpect(jsonPath("$.email").value("test.user@example.com"))
                .andExpect(jsonPath("$.firstName").value("First Name"))
                .andExpect(jsonPath("$.lastName").value("Last Name"))
                .andExpect(jsonPath("$.userRole").value(UserRole.CLIENT.name()))
                .andExpect(jsonPath("$.isEnabled").value(true))
                .andExpect(jsonPath("$.isNonLocked").value(true))
                .andExpect(jsonPath("$.registeredAt").exists());

        verify(authService, times(1)).registerUser(eq(registerRequest));
    }

    @Test
    void registerUser_shouldReturnBadRequest_whenEmailIsBlank() throws Exception {

        UserRegisterRequest invalidRequest = UserRegisterRequest.builder()
                .email("")
                .password("Password123!")
                .confirmPassword("Password123!")
                .firstName("First Name")
                .lastName("Last Name")
                .build();

        mockMvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest))
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("MethodArgumentNotValidException"))
                .andExpect(jsonPath("$.details", containsInAnyOrder("Email is required", "Invalid email")))
                .andExpect(jsonPath("$.path").value("/auth/register"))
                .andExpect(jsonPath("$.timestamp").exists());

        verify(authService, never()).registerUser(any());
    }

    @Test
    void registerUser_shouldReturnBadRequest_whenEmailIsInvalidFormat() throws Exception {

        UserRegisterRequest invalidRequest = UserRegisterRequest.builder()
                .email("INVALID_EMAIL")
                .password("Password123!")
                .confirmPassword("Password123!")
                .firstName("First Name")
                .lastName("Last Name")
                .build();

        mockMvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest))
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("MethodArgumentNotValidException"))
                .andExpect(jsonPath("$.details", containsInAnyOrder("Invalid email")))
                .andExpect(jsonPath("$.path").value("/auth/register"))
                .andExpect(jsonPath("$.timestamp").exists());

        verify(authService, never()).registerUser(any());
    }

    @Test
    void registerUser_shouldReturnBadRequest_whenPasswordIsBlank() throws Exception {

        UserRegisterRequest invalidRequest = UserRegisterRequest.builder()
                .email("test.user@example.com")
                .password("")
                .confirmPassword("Password123!")
                .firstName("First Name")
                .lastName("Last Name")
                .build();

        mockMvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest))
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("MethodArgumentNotValidException"))
                .andExpect(jsonPath("$.details", containsInAnyOrder("Password is required", "Invalid value for password: Must contain at least one digit, one lowercase letter, one uppercase letter, one special character, no whitespace, and be at least 8 characters long")))
                .andExpect(jsonPath("$.path").value("/auth/register"))
                .andExpect(jsonPath("$.timestamp").exists());

        verify(authService, never()).registerUser(any());
    }

    @Test
    void registerUser_shouldReturnBadRequest_whenPasswordDoesNotMatchRegex() throws Exception {

        UserRegisterRequest invalidRequest = UserRegisterRequest.builder()
                .email("test.user@example.com")
                .password("INVALID_PASSWORD")
                .confirmPassword("Password123!")
                .firstName("First Name")
                .lastName("Last Name")
                .build();

        mockMvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest))
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("MethodArgumentNotValidException"))
                .andExpect(jsonPath("$.details", containsInAnyOrder("Invalid value for password: Must contain at least one digit, one lowercase letter, one uppercase letter, one special character, no whitespace, and be at least 8 characters long")))
                .andExpect(jsonPath("$.path").value("/auth/register"))
                .andExpect(jsonPath("$.timestamp").exists());

        verify(authService, never()).registerUser(any());
    }

    @Test
    void registerUser_shouldReturnBadRequest_whenConfirmPasswordIsBlank() throws Exception {

        UserRegisterRequest invalidRequest = UserRegisterRequest.builder()
                .email("test.user@example.com")
                .password("Password123!")
                .confirmPassword("")
                .firstName("First Name")
                .lastName("Last Name")
                .build();

        mockMvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest))
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("MethodArgumentNotValidException"))
                .andExpect(jsonPath("$.details", containsInAnyOrder("Confirm password field is required", "Invalid value for confirm password: Must contain at least one digit, one lowercase letter, one uppercase letter, one special character, no whitespace, and be at least 8 characters long")))
                .andExpect(jsonPath("$.path").value("/auth/register"))
                .andExpect(jsonPath("$.timestamp").exists());

        verify(authService, never()).registerUser(any());
    }

    @Test
    void registerUser_shouldReturnBadRequest_whenConfirmPasswordDoesNotMatchRegex() throws Exception {

        UserRegisterRequest invalidRequest = UserRegisterRequest.builder()
                .email("test.user@example.com")
                .password("Password123!")
                .confirmPassword("INVALID_PASSWORD")
                .firstName("First Name")
                .lastName("Last Name")
                .build();

        mockMvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest))
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("MethodArgumentNotValidException"))
                .andExpect(jsonPath("$.details", containsInAnyOrder("Invalid value for confirm password: Must contain at least one digit, one lowercase letter, one uppercase letter, one special character, no whitespace, and be at least 8 characters long")))
                .andExpect(jsonPath("$.path").value("/auth/register"))
                .andExpect(jsonPath("$.timestamp").exists());

        verify(authService, never()).registerUser(any());
    }

    @Test
    void registerUser_shouldReturnBadRequest_whenFirstNameIsBlank() throws Exception {

        UserRegisterRequest invalidRequest = UserRegisterRequest.builder()
                .email("test.user@example.com")
                .password("Password123!")
                .confirmPassword("Password123!")
                .firstName("")
                .lastName("Last Name")
                .build();

        mockMvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest))
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("MethodArgumentNotValidException"))
                .andExpect(jsonPath("$.details", containsInAnyOrder("First name is required", "Invalid first name: Must be of 2 - 30 characters")))
                .andExpect(jsonPath("$.path").value("/auth/register"))
                .andExpect(jsonPath("$.timestamp").exists());

        verify(authService, never()).registerUser(any());
    }

    @Test
    void registerUser_shouldReturnBadRequest_whenFirstNameIsTooShort() throws Exception {

        UserRegisterRequest invalidRequest = UserRegisterRequest.builder()
                .email("test.user@example.com")
                .password("Password123!")
                .confirmPassword("Password123!")
                .firstName("A")
                .lastName("Last Name")
                .build();

        mockMvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest))
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("MethodArgumentNotValidException"))
                .andExpect(jsonPath("$.details", containsInAnyOrder("Invalid first name: Must be of 2 - 30 characters")))
                .andExpect(jsonPath("$.path").value("/auth/register"))
                .andExpect(jsonPath("$.timestamp").exists());

        verify(authService, never()).registerUser(any());
    }

    @Test
    void registerUser_shouldReturnBadRequest_whenFirstNameIsTooLong() throws Exception {

        UserRegisterRequest invalidRequest = UserRegisterRequest.builder()
                .email("test.user@example.com")
                .password("Password123!")
                .confirmPassword("Password123!")
                .firstName("A".repeat(31))
                .lastName("Last Name")
                .build();

        mockMvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest))
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("MethodArgumentNotValidException"))
                .andExpect(jsonPath("$.details", containsInAnyOrder("Invalid first name: Must be of 2 - 30 characters")))
                .andExpect(jsonPath("$.path").value("/auth/register"))
                .andExpect(jsonPath("$.timestamp").exists());

        verify(authService, never()).registerUser(any());
    }

    @Test
    void registerUser_shouldReturnBadRequest_whenLastNameIsBlank() throws Exception {

        UserRegisterRequest invalidRequest = UserRegisterRequest.builder()
                .email("test.user@example.com")
                .password("Password123!")
                .confirmPassword("Password123!")
                .firstName("First Name")
                .lastName("")
                .build();

        mockMvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest))
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("MethodArgumentNotValidException"))
                .andExpect(jsonPath("$.details", containsInAnyOrder("Last name is required", "Invalid last name: Must be of 2 - 30 characters")))
                .andExpect(jsonPath("$.path").value("/auth/register"))
                .andExpect(jsonPath("$.timestamp").exists());

        verify(authService, never()).registerUser(any());
    }


    @Test
    void registerUser_shouldReturnBadRequest_whenLastNameIsTooShort() throws Exception {

        UserRegisterRequest invalidRequest = UserRegisterRequest.builder()
                .email("test.user@example.com")
                .password("Password123!")
                .confirmPassword("Password123!")
                .firstName("First Name")
                .lastName("A")
                .build();

        mockMvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest))
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("MethodArgumentNotValidException"))
                .andExpect(jsonPath("$.details", containsInAnyOrder("Invalid last name: Must be of 2 - 30 characters")))
                .andExpect(jsonPath("$.path").value("/auth/register"))
                .andExpect(jsonPath("$.timestamp").exists());

        verify(authService, never()).registerUser(any());
    }

    @Test
    void registerUser_shouldReturnBadRequest_whenLastNameIsTooLong() throws Exception {

        UserRegisterRequest invalidRequest = UserRegisterRequest.builder()
                .email("test.user@example.com")
                .password("Password123!")
                .confirmPassword("Password123!")
                .firstName("First Name")
                .lastName("A".repeat(31))
                .build();

        mockMvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest))
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("MethodArgumentNotValidException"))
                .andExpect(jsonPath("$.details", containsInAnyOrder("Invalid last name: Must be of 2 - 30 characters")))
                .andExpect(jsonPath("$.path").value("/auth/register"))
                .andExpect(jsonPath("$.timestamp").exists());

        verify(authService, never()).registerUser(any());
    }

    @Test
    void login_shouldReturnOk_whenValidCredentials() throws Exception {

        LoginRequest loginRequest = LoginRequest.builder()
                .email("user@example.com")
                .password("SecurePassword123!")
                .build();
        
        LoginResponse expectedResponse = LoginResponse.builder()
                .accessToken("mockedAccessToken123")
                .refreshToken("mockedRefreshToken456")
                .build();
        
        when(authService.login(eq(loginRequest))).thenReturn(expectedResponse);

        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest))
                        .accept(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").value("mockedAccessToken123"))
                .andExpect(jsonPath("$.refreshToken").value("mockedRefreshToken456"));

        verify(authService, times(1)).login(eq(loginRequest));
    }

    @Test
    void login_shouldReturnBadRequest_whenEmailIsBlank() throws Exception {

        LoginRequest invalidRequest = LoginRequest.builder()
                .email("")
                .password("SecurePassword123!")
                .build();

        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest))
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("MethodArgumentNotValidException"))
                .andExpect(jsonPath("$.details", containsInAnyOrder("Email is required", "Invalid email")))
                .andExpect(jsonPath("$.path").value("/auth/login"))
                .andExpect(jsonPath("$.timestamp").exists());

        verify(authService, never()).login(any());
    }

    @Test
    void login_shouldReturnBadRequest_whenEmailIsInvalidFormat() throws Exception {

        LoginRequest invalidRequest = LoginRequest.builder()
                .email("INVALID_EMAIL")
                .password("SecurePassword123!")
                .build();

        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest))
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("MethodArgumentNotValidException"))
                .andExpect(jsonPath("$.details", containsInAnyOrder("Invalid email")))
                .andExpect(jsonPath("$.path").value("/auth/login"))
                .andExpect(jsonPath("$.timestamp").exists());

        verify(authService, never()).login(any());
    }

    @Test
    void login_shouldReturnBadRequest_whenPasswordIsBlank() throws Exception {

        LoginRequest invalidRequest = LoginRequest.builder()
                .email("user@example.com")
                .password("")
                .build();

        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest))
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("MethodArgumentNotValidException"))
                .andExpect(jsonPath("$.details", containsInAnyOrder("Password is required", "Invalid value for password: Must contain at least one digit, one lowercase letter, one uppercase letter, one special character, no whitespace, and be at least 8 characters long")))
                .andExpect(jsonPath("$.path").value("/auth/login"))
                .andExpect(jsonPath("$.timestamp").exists());

        verify(authService, never()).login(any());
    }

    @Test
    void login_shouldReturnBadRequest_whenPasswordIsInvalidFormat() throws Exception {

        LoginRequest invalidRequest = LoginRequest.builder()
                .email("user@example.com")
                .password("INVALID_PASSWORD")
                .build();

        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest))
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("MethodArgumentNotValidException"))
                .andExpect(jsonPath("$.details", containsInAnyOrder("Invalid value for password: Must contain at least one digit, one lowercase letter, one uppercase letter, one special character, no whitespace, and be at least 8 characters long")))
                .andExpect(jsonPath("$.path").value("/auth/login"))
                .andExpect(jsonPath("$.timestamp").exists());

        verify(authService, never()).login(any());
    }

    @Test
    void getNewAccessToken_shouldReturnOk_whenValidRefreshToken() throws Exception {

        String mockRefreshToken = "abc123_DEF-456.ghi789-JKL_mno.OPQ_rstUVW-xyz";

        RefreshRequest refreshRequest = RefreshRequest.builder()
                .refreshToken(mockRefreshToken)
                .build();
        
        RefreshResponse expectedResponse = RefreshResponse.builder()
                .accessToken("mockedAccessToken123")
                .build();

        when(authService.getNewAccessToken(any(RefreshRequest.class))).thenReturn(expectedResponse);

        mockMvc.perform(post("/auth/token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(refreshRequest))
                        .accept(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.type").value("Bearer"))
                .andExpect(jsonPath("$.accessToken").value("mockedAccessToken123"));

        verify(authService, times(1)).getNewAccessToken(any(RefreshRequest.class));
    }

    @Test
    void getNewAccessToken_shouldReturnBadRequest_whenRefreshTokenIsBlank() throws Exception {

        RefreshRequest invalidRequest = RefreshRequest.builder()
                .refreshToken("")
                .build();

        mockMvc.perform(post("/auth/token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest))
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("MethodArgumentNotValidException"))
                .andExpect(jsonPath("$.details", containsInAnyOrder("Refresh token is required", "Invalid refresh token format")))
                .andExpect(jsonPath("$.path").value("/auth/token"))
                .andExpect(jsonPath("$.timestamp").exists());

        verify(authService, never()).getNewAccessToken(any());
    }

    @Test
    void getNewAccessToken_shouldReturnBadRequest_whenRefreshTokenIsInvalidFormat() throws Exception {

        RefreshRequest invalidRequest = RefreshRequest.builder()
                .refreshToken("INVALID_TOKEN")
                .build();

        mockMvc.perform(post("/auth/token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest))
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("MethodArgumentNotValidException"))
                .andExpect(jsonPath("$.details", containsInAnyOrder("Invalid refresh token format")))
                .andExpect(jsonPath("$.path").value("/auth/token"))
                .andExpect(jsonPath("$.timestamp").exists());

        verify(authService, never()).getNewAccessToken(any());
    }

    @Test
    void forgotPassword_shouldReturnOk_whenValidEmail() throws Exception {

        ForgotPasswordRequest resetRequest = ForgotPasswordRequest.builder()
                .email("test.user@example.com")
                .build();

        MessageResponse messageResponse = MessageResponse.builder()
                .message("Password reset link sent to user's email.")
                .build();

        when(authService.forgotPassword(eq(resetRequest))).thenReturn(messageResponse);

        mockMvc.perform(post("/auth/forgot-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(resetRequest))
                        .accept(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Password reset link sent to user's email."));

        verify(authService, times(1)).forgotPassword(eq(resetRequest));
    }

    @Test
    void forgotPassword_shouldReturnBadRequest_whenEmailIsBlank() throws Exception {

        ForgotPasswordRequest invalidRequest = ForgotPasswordRequest.builder()
                .email("")
                .build();

        mockMvc.perform(post("/auth/forgot-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest))
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("MethodArgumentNotValidException"))
                .andExpect(jsonPath("$.details", containsInAnyOrder("Email is required", "Invalid email format")))
                .andExpect(jsonPath("$.path").value("/auth/forgot-password"))
                .andExpect(jsonPath("$.timestamp").exists());

        verify(authService, never()).forgotPassword(any());
    }

    @Test
    void forgotPassword_shouldReturnBadRequest_whenEmailIsInvalidFormat() throws Exception {

        ForgotPasswordRequest invalidRequest = ForgotPasswordRequest.builder()
                .email("INVALID_EMAIL")
                .build();

        mockMvc.perform(post("/auth/forgot-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest))
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("MethodArgumentNotValidException"))
                .andExpect(jsonPath("$.details", containsInAnyOrder("Invalid email format")))
                .andExpect(jsonPath("$.path").value("/auth/forgot-password"))
                .andExpect(jsonPath("$.timestamp").exists());

        verify(authService, never()).forgotPassword(any());
    }

    @Test
    void resetPassword_shouldReturnOk_whenValidPasswordResetRequest() throws Exception {

        String mockPasswordResetToken = "abc123_DEF-456.ghi789-JKL_mno.OPQ_rstUVW-xyz";

        PasswordResetRequest resetRequest = PasswordResetRequest.builder()
                .passwordResetToken(mockPasswordResetToken)
                .newPassword("NewPassword123!")
                .confirmPassword("NewPassword123!")
                .build();


        MessageResponse messageResponse = MessageResponse.builder()
                .message("Password has been successfully reset.")
                .build();

        when(authService.resetPassword(eq(resetRequest))).thenReturn(messageResponse);

        mockMvc.perform(post("/auth/reset-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(resetRequest))
                        .accept(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Password has been successfully reset."));

        verify(authService, times(1)).resetPassword(eq(resetRequest));
    }

    @Test
    void resetPassword_shouldReturnBadRequest_whenTokenIsBlank() throws Exception {

        PasswordResetRequest invalidRequest = PasswordResetRequest.builder()
                .passwordResetToken("")
                .newPassword("NewPassword123!")
                .confirmPassword("NewPassword123!")
                .build();

        mockMvc.perform(post("/auth/reset-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest))
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("MethodArgumentNotValidException"))
                .andExpect(jsonPath("$.details", containsInAnyOrder("Token is required to reset password", "Invalid reset password token format")))
                .andExpect(jsonPath("$.path").value("/auth/reset-password"))
                .andExpect(jsonPath("$.timestamp").exists());

        verify(authService, never()).resetPassword(any());
    }

    @Test
    void resetPassword_shouldReturnBadRequest_whenTokenIsInvalidFormat() throws Exception {

        PasswordResetRequest invalidRequest = PasswordResetRequest.builder()
                .passwordResetToken("INVALID_TOKEN")
                .newPassword("NewPassword123!")
                .confirmPassword("NewPassword123!")
                .build();

        mockMvc.perform(post("/auth/reset-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest))
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("MethodArgumentNotValidException"))
                .andExpect(jsonPath("$.details", containsInAnyOrder("Invalid reset password token format")))
                .andExpect(jsonPath("$.path").value("/auth/reset-password"))
                .andExpect(jsonPath("$.timestamp").exists());

        verify(authService, never()).forgotPassword(any());
    }

    @Test
    void resetPassword_shouldReturnBadRequest_whenNewPasswordIsBlank() throws Exception {

        String mockPasswordResetToken = "abc123_DEF-456.ghi789-JKL_mno.OPQ_rstUVW-xyz";

        PasswordResetRequest invalidRequest = PasswordResetRequest.builder()
                .passwordResetToken(mockPasswordResetToken)
                .newPassword("")
                .confirmPassword("NewPassword123!")
                .build();

        mockMvc.perform(post("/auth/reset-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest))
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("MethodArgumentNotValidException"))
                .andExpect(jsonPath("$.details", containsInAnyOrder("New password is required", "Invalid value for 'new password': Must contain at least one digit, one lowercase letter, one uppercase letter, one special character, no whitespace, and be at least 8 characters long")))
                .andExpect(jsonPath("$.path").value("/auth/reset-password"))
                .andExpect(jsonPath("$.timestamp").exists());

        verify(authService, never()).resetPassword(any());
    }

    @Test
    void resetPassword_shouldReturnBadRequest_whenNewPasswordIsInvalidFormat() throws Exception {

        String mockPasswordResetToken = "abc123_DEF-456.ghi789-JKL_mno.OPQ_rstUVW-xyz";

        PasswordResetRequest invalidRequest = PasswordResetRequest.builder()
                .passwordResetToken(mockPasswordResetToken)
                .newPassword("INVALID_EMAIL")
                .confirmPassword("NewPassword123!")
                .build();

        mockMvc.perform(post("/auth/reset-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest))
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("MethodArgumentNotValidException"))
                .andExpect(jsonPath("$.details", containsInAnyOrder("Invalid value for 'new password': Must contain at least one digit, one lowercase letter, one uppercase letter, one special character, no whitespace, and be at least 8 characters long")))
                .andExpect(jsonPath("$.path").value("/auth/reset-password"))
                .andExpect(jsonPath("$.timestamp").exists());

        verify(authService, never()).forgotPassword(any());
    }

    @Test
    void resetPassword_shouldReturnBadRequest_whenConfirmNewPasswordIsBlank() throws Exception {

        String mockPasswordResetToken = "abc123_DEF-456.ghi789-JKL_mno.OPQ_rstUVW-xyz";

        PasswordResetRequest invalidRequest = PasswordResetRequest.builder()
                .passwordResetToken(mockPasswordResetToken)
                .newPassword("NewPassword123!")
                .confirmPassword("")
                .build();

        mockMvc.perform(post("/auth/reset-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest))
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("MethodArgumentNotValidException"))
                .andExpect(jsonPath("$.details", containsInAnyOrder("Confirm new password field is required", "Invalid value for 'confirm new password': Must contain at least one digit, one lowercase letter, one uppercase letter, one special character, no whitespace, and be at least 8 characters long")))
                .andExpect(jsonPath("$.path").value("/auth/reset-password"))
                .andExpect(jsonPath("$.timestamp").exists());

        verify(authService, never()).resetPassword(any());
    }

    @Test
    void resetPassword_shouldReturnBadRequest_whenConfirmNewPasswordIsInvalidFormat() throws Exception {

        String mockPasswordResetToken = "abc123_DEF-456.ghi789-JKL_mno.OPQ_rstUVW-xyz";

        PasswordResetRequest invalidRequest = PasswordResetRequest.builder()
                .passwordResetToken(mockPasswordResetToken)
                .newPassword("NewPassword123!")
                .confirmPassword("INVALID_EMAIL")
                .build();

        mockMvc.perform(post("/auth/reset-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest))
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("MethodArgumentNotValidException"))
                .andExpect(jsonPath("$.details", containsInAnyOrder("Invalid value for 'confirm new password': Must contain at least one digit, one lowercase letter, one uppercase letter, one special character, no whitespace, and be at least 8 characters long")))
                .andExpect(jsonPath("$.path").value("/auth/reset-password"))
                .andExpect(jsonPath("$.timestamp").exists());

        verify(authService, never()).forgotPassword(any());
    }
}