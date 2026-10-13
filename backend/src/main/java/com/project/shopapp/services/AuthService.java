package com.project.shopapp.services;

import com.project.shopapp.dtos.request.LoginRequest;
import com.project.shopapp.dtos.request.RegisterRequest;
import com.project.shopapp.dtos.response.AuthResponse;
import com.project.shopapp.dtos.response.UserResponse;

public interface AuthService {
    AuthResponse register(RegisterRequest request);

    AuthResponse login(LoginRequest request);

    UserResponse currentUser(String phoneNumber);

    void logout(String token);
}
