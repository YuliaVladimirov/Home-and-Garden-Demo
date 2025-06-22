package org.example.homeandgarden.security.service;

import org.example.homeandgarden.security.dto.LoginRequest;
import org.example.homeandgarden.security.dto.LoginResponse;
import org.example.homeandgarden.security.dto.RefreshRequest;
import org.example.homeandgarden.security.dto.RefreshResponse;
import org.example.homeandgarden.user.dto.UserRegisterRequest;
import org.example.homeandgarden.user.dto.UserResponse;

public interface AuthService {

    UserResponse registerUser(UserRegisterRequest userRegisterRequest);
    LoginResponse login(LoginRequest loginRequest);
    RefreshResponse getNewAccessToken(RefreshRequest refreshRequest);
}
