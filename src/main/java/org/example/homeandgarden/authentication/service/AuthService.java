package org.example.homeandgarden.authentication.service;

import org.example.homeandgarden.authentication.dto.LoginRequest;
import org.example.homeandgarden.authentication.dto.LoginResponse;
import org.example.homeandgarden.authentication.dto.RefreshRequest;
import org.example.homeandgarden.authentication.dto.RefreshResponse;
import org.example.homeandgarden.user.dto.UserRegisterRequest;
import org.example.homeandgarden.user.dto.UserResponse;

public interface AuthService {

    UserResponse registerUser(UserRegisterRequest userRegisterRequest);
    LoginResponse login(LoginRequest loginRequest);
    RefreshResponse getNewAccessToken(RefreshRequest refreshRequest);
}
