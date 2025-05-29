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

        if (accessToken == null) {
            throw new BadCredentialsException("Access token is null.");
        }

        if(jwtService.isAccessTokenValid(accessToken)) {
            String email = jwtService.getUserEmailFromAccessToken(accessToken);

            User existingUser = userRepository.findByEmail(email).orElseThrow(() -> new DataNotFoundException((String.format("User with email: %s, was not found.", email))));
            existingUser.setRefreshToken(null);
            User updatedUser = userRepository.saveAndFlush(existingUser);

            if (updatedUser.getRefreshToken() != null) {
                throw new IllegalStateException("Something went wrong, refresh token is not deleted from database.");
            }
        }
    }
}

