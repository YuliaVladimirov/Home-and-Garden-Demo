package org.example.homeandgarden.authentication.service;

import org.example.homeandgarden.authentication.dto.*;
import org.example.homeandgarden.shared.MessageResponse;
import org.example.homeandgarden.user.dto.UserRegisterRequest;
import org.example.homeandgarden.user.dto.UserResponse;

public interface AuthService {

    UserResponse registerUser(UserRegisterRequest userRegisterRequest);
    LoginResponse login(LoginRequest loginRequest);
    RefreshResponse getNewAccessToken(RefreshRequest refreshRequest);
    MessageResponse forgotPassword(ForgotPasswordRequest request);
    MessageResponse resetPassword(PasswordResetRequest resetRequest);
}
