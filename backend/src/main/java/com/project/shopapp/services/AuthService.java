package com.project.shopapp.services;

import com.project.shopapp.dtos.LoginRequest;
import com.project.shopapp.dtos.RegisterRequest;
import com.project.shopapp.responses.AuthResponse;
import com.project.shopapp.responses.UserResponse;

public interface AuthService {
    AuthResponse register(RegisterRequest request);

    AuthResponse login(LoginRequest request);

    UserResponse currentUser(String phoneNumber);

    void logout(String token);
}
