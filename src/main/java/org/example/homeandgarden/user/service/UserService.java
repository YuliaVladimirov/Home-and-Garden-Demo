package org.example.homeandgarden.user.service;

import org.example.homeandgarden.shared.MessageResponse;
import org.example.homeandgarden.user.dto.*;
import org.springframework.data.domain.Page;

public interface UserService {

    Page<UserResponse> getUsersByStatus(Boolean isEnabled, Boolean isNonLocked, Integer size, Integer page, String order, String sortBy);
    UserResponse getUserById(String userId);
    UserResponse getMyProfile(String userEmail);
    UserResponse updateMyProfile(String email, UserUpdateRequest userUpdateRequest);
    MessageResponse changeMyPassword(String email, ChangePasswordRequest changePasswordRequest);
    MessageResponse setUserRole(String userId, String role);
    MessageResponse toggleUserLockState(String userId);
    MessageResponse unregisterMyAccount(String email, UserUnregisterRequest userUnregisterRequest);
}
