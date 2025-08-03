package org.example.homeandgarden.authentication.controller;

import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import jakarta.validation.Valid;
import org.example.homeandgarden.authentication.dto.LoginRequest;
import org.example.homeandgarden.authentication.dto.RefreshRequest;
import org.example.homeandgarden.authentication.dto.LoginResponse;
import org.example.homeandgarden.authentication.dto.RefreshResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.example.homeandgarden.authentication.service.AuthService;
import org.example.homeandgarden.shared.ErrorResponse;
import org.example.homeandgarden.swagger.GroupFourErrorResponses;
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
@Tag(name = "Authentication controller", description = "Controller for authentication of user and getting/refreshing JWTs")
public class AuthController {

    private final AuthService authService;


    @Operation(summary = "Register user", description = "Creates a new user account in the system. The user's registration details are provided in the request body." +
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

    @Operation(summary = "User login", description = "Authenticates a user and provides an authentication token upon successful login. User's credentials (e.g., email and password) should be provided in the request body.")
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

    @Operation(summary = "Refresh access token", description = "Obtains a new access token using a valid refresh token.")
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


}