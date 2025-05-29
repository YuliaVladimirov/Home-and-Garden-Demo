package org.example.homeandgarden.security.service;

import org.example.homeandgarden.security.dto.LoginRequest;
import org.example.homeandgarden.security.dto.LoginResponse;
import org.example.homeandgarden.security.dto.RefreshRequest;
import org.example.homeandgarden.security.dto.RefreshResponse;

public interface AuthService {

    LoginResponse login(LoginRequest loginRequest);
    RefreshResponse getNewAccessToken(RefreshRequest refreshRequest);
}
