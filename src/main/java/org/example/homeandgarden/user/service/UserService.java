package org.example.homeandgarden.user.service;

import org.example.homeandgarden.shared.MessageResponse;
import org.example.homeandgarden.user.dto.*;
import org.springframework.data.web.PagedModel;

public interface UserService {

    PagedModel<UserResponse> getAllUsers(Boolean isEnabled, Integer size, Integer page, String order, String sortBy);
    UserResponse getUserById(String userId);
    UserResponse registerUser(UserRegisterRequest userRegisterRequest);
    MessageResponse unregisterUser(String userId, UserUnregisterRequest userUnregisterRequest);
    UserResponse updateUser(String id, UserUpdateRequest userUpdateRequest);
    MessageResponse setUserRole(String userId, String role);
    MessageResponse toggleLockState(String userId);
}
