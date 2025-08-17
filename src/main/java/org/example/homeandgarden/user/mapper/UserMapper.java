package org.example.homeandgarden.user.mapper;

import lombok.RequiredArgsConstructor;
import org.example.homeandgarden.user.dto.UserRegisterRequest;
import org.example.homeandgarden.user.dto.UserResponse;
import org.example.homeandgarden.user.entity.User;
import org.example.homeandgarden.user.entity.enums.UserRole;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class UserMapper {

    public User createRequestToUser(
            UserRegisterRequest userRegisterRequest) {

        return User.builder()
                .email(userRegisterRequest.getEmail())
                .firstName(userRegisterRequest.getFirstName())
                .lastName(userRegisterRequest.getLastName())
                .userRole(UserRole.CLIENT)
                .build();
    }

    public UserResponse userToResponse(User user) {

        return UserResponse.builder()
                .userId(user.getUserId())
                .email(user.getEmail())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .userRole(user.getUserRole())
                .isEnabled(user.getIsEnabled())
                .isNonLocked(user.getIsNonLocked())
                .registeredAt(user.getRegisteredAt())
                .updatedAt(user.getUpdatedAt())
                .build();
    }

    public UserResponse userToResponseDetailed(User user) {

        return UserResponse.builder()
                .userId(user.getUserId())
                .email(user.getEmail())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .userRole(user.getUserRole())
                .isEnabled(user.getIsEnabled())
                .isNonLocked(user.getIsNonLocked())
                .registeredAt(user.getRegisteredAt())
                .updatedAt(user.getUpdatedAt())
                .build();
    }
}
