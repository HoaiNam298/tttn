package com.project.shopapp.services;

import com.project.shopapp.dtos.request.RegisterRequest;
import com.project.shopapp.dtos.response.UserResponse;

public interface AuthService {
    UserResponse register(RegisterRequest request);

    UserResponse currentUser(String subject, boolean admin);
}
