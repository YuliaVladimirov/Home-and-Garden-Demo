package org.example.homeandgarden.user.service;

import org.example.homeandgarden.shared.MessageResponse;
import org.example.homeandgarden.user.dto.*;
import org.springframework.data.domain.Page;

public interface UserService {

    Page<UserResponse> getUsersByStatus(Boolean isEnabled, Boolean isNonLocked, Integer size, Integer page, String order, String sortBy);
    UserResponse getUserById(String userId);
    UserResponse getMyProfile(String userEmail);
    UserResponse updateUser(String id, UserUpdateRequest userUpdateRequest);
    MessageResponse setUserRole(String userId, String role);
    MessageResponse toggleLockState(String userId);
    MessageResponse unregisterMyAccount(String email, UserUnregisterRequest userUnregisterRequest);
    MessageResponse changeMyPassword(String email, ChangePasswordRequest changePasswordRequest);

}
