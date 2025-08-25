package org.example.homeandgarden.authentication.controller;

import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import org.example.homeandgarden.authentication.dto.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.example.homeandgarden.authentication.service.AuthService;
import org.example.homeandgarden.shared.ErrorResponse;
import org.example.homeandgarden.shared.MessageResponse;
import org.example.homeandgarden.swagger.GroupFourErrorResponses;
import org.example.homeandgarden.swagger.GroupOneErrorResponses;
import org.example.homeandgarden.swagger.GroupThreeErrorResponses;
import org.example.homeandgarden.user.dto.UserRegisterRequest;
import org.example.homeandgarden.user.dto.UserResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
@Tag(name = "Authentication controller", description = "Provides endpoints for user authentication, issuing JWT tokens, and refreshing expired tokens.")
public class AuthController {

    private final AuthService authService;


// 🌐 Public access endpoints — no authentication required (accessible to all users)

    @Operation(summary = "Register a new user account.", description = "Creates a new user account in the system. The user's registration details are provided in the request body." +
            "    ")
    @ApiResponse(responseCode = "201", description = "User successfully registered.", content = @Content(mediaType = "application/json", schema = @Schema(implementation = UserResponse.class)))
    @ApiResponse(responseCode = "409", description = "Conflict: A user with the provided email already exists.", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class)))
    @GroupFourErrorResponses
    @PreAuthorize("permitAll()")
    @PostMapping("/register")
    public ResponseEntity<UserResponse> registerUser(

            @RequestBody
            @Valid
            UserRegisterRequest userRegisterRequest) {

        UserResponse registeredUser = authService.registerUser(userRegisterRequest);
        return new ResponseEntity<>(registeredUser, HttpStatus.CREATED);
    }

    @Operation(summary = "Authenticate user and generate JWT tokens.", description = "Validates user credentials and returns an access token and refresh token for authenticated sessions. User's credentials (e.g., email and password) should be provided in the request body.")
    @ApiResponse(responseCode = "200", description = "Login successful. Returns authentication access and refresh tokens.", content = @Content(mediaType = "application/json", schema = @Schema(implementation = LoginResponse.class)))
    @GroupThreeErrorResponses
    @PreAuthorize("permitAll()")
    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(

            @RequestBody
            @Valid
            LoginRequest loginRequest) {

        LoginResponse loginResponse = authService.login(loginRequest);
        return new ResponseEntity<>(loginResponse, HttpStatus.OK);
    }

    @Operation(summary = "Refresh JWT access token", description = "Generates a new access token using a valid refresh token without requiring user credentials again.")
    @ApiResponse(responseCode = "200", description = "Access token successfully refreshed.", content = @Content(mediaType = "application/json", schema = @Schema(implementation = RefreshResponse.class)))
    @GroupFourErrorResponses
    @PreAuthorize("permitAll()")
    @PostMapping("/token")
    public ResponseEntity<RefreshResponse> getNewAccessToken(

            @RequestBody
            @Valid
            RefreshRequest refreshRequest) {

        RefreshResponse refreshResponse = authService.getNewAccessToken(refreshRequest);
        return new ResponseEntity<>(refreshResponse, HttpStatus.OK);
    }

    @Operation(summary = "Initiate password reset process", description = "Sends a password reset link to the user's email if the email is registered in the system.")
    @ApiResponse(responseCode = "200", description = "Password reset link sent.", content = @Content(mediaType = "application/json", schema = @Schema(implementation = RefreshResponse.class)))
    @GroupFourErrorResponses
    @PreAuthorize("permitAll()")
    @PostMapping("/forgot-password")
    public ResponseEntity<MessageResponse> forgotPassword(

            @RequestBody
            @Valid
            ForgotPasswordRequest request) {

        MessageResponse messageResponse = authService.forgotPassword(request);
        return new ResponseEntity<>(messageResponse, HttpStatus.OK);
    }

    @Operation(summary = "Reset password", description = "Resets password using a valid reset password token and the new password provided.")
    @ApiResponse(responseCode = "200", description = "Password successfully reset.", content = @Content(mediaType = "application/json", schema = @Schema(implementation = RefreshResponse.class)))
    @GroupFourErrorResponses
    @PreAuthorize("permitAll()")
    @PostMapping("/reset-password")
    public ResponseEntity<MessageResponse> resetPassword(

            @RequestBody
            @Valid
            PasswordResetRequest resetRequest) {

        MessageResponse messageResponse = authService.resetPassword(resetRequest);
        return new ResponseEntity<>(messageResponse, HttpStatus.OK);
    }


// 🔐 Self-access endpoints — available only to the authenticated user (operates on their own data)

// This placeholder method exists only to document the endpoint in Swagger UI. Actual logout is handled by Spring Security via CustomLogoutHandler.
    @Operation(summary = "Logout user", description = "Triggers user logout handled by Spring Security. Invalidates JWT tokens and clears the security context.")
    @ApiResponse(responseCode = "200", description = "Logout successful", content = @Content(mediaType = "application/json", schema = @Schema(implementation = MessageResponse.class)))
    @GroupOneErrorResponses
    @SecurityRequirement(name = "JWT")
    @PreAuthorize("isAuthenticated()")
    @PostMapping("/logout")
    public void logout() {}

}