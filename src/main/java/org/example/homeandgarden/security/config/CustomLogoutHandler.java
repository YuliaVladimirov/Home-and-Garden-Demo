package org.example.homeandgarden.security.config;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.AllArgsConstructor;

import org.example.homeandgarden.exception.DataNotFoundException;
import org.example.homeandgarden.user.entity.User;
import org.example.homeandgarden.user.repository.UserRepository;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.logout.LogoutHandler;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@AllArgsConstructor
public class CustomLogoutHandler implements LogoutHandler {

    private final UserRepository userRepository;
    private final JwtService jwtService;

    @Transactional
    @Override
    public void logout(HttpServletRequest request, HttpServletResponse response, Authentication authentication) {

        String accessToken = jwtService.getTokenFromRequestHeader(request);

        if (accessToken == null || accessToken.isBlank()) {
            throw new BadCredentialsException("Access token is missing or empty.");
        }

        if (!jwtService.isAccessTokenValid(accessToken)) {
            throw new BadCredentialsException("Invalid or expired access token.");
        }

        String email = jwtService.getUserEmailFromAccessToken(accessToken);
        if (email == null) {
            throw new BadCredentialsException("Token does not contain a user identifier.");
        }

        User existingUser = userRepository.findByEmail(email).orElseThrow(() -> new DataNotFoundException((String.format("User with email: %s, was not found.", email))));

        if (existingUser.getRefreshToken() == null) {
            throw new BadCredentialsException("User is already logged out. No action needed.");
        }

        existingUser.setRefreshToken(null);
        userRepository.save(existingUser);
    }
}

