package com.project.shopapp.services;

import com.project.shopapp.dtos.request.ChangePasswordRequest;
import com.project.shopapp.dtos.request.UpdateProfileRequest;
import com.project.shopapp.dtos.response.UserResponse;

public interface AccountSettingsService {
    UserResponse updateProfile(String subject, String role, UpdateProfileRequest request);

    void changePassword(String subject, ChangePasswordRequest request);

    void synchronizePending();
}
