package com.project.shopapp.controller;

import com.project.shopapp.components.ShopAppAuthenticationToken;
import com.project.shopapp.dtos.request.ChangePasswordRequest;
import com.project.shopapp.dtos.request.RegisterRequest;
import com.project.shopapp.dtos.request.UpdateProfileRequest;
import com.project.shopapp.dtos.response.UserResponse;
import com.project.shopapp.services.AccountSettingsService;
import com.project.shopapp.services.AuthService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class AuthController {
    private final AuthService service;
    private final AccountSettingsService settings;

    public AuthController(AuthService service, AccountSettingsService settings) {
        this.service = service;
        this.settings = settings;
    }

    @PostMapping("/api/v1/auth/register")
    @ResponseStatus(HttpStatus.CREATED)
    public UserResponse register(@Valid @RequestBody RegisterRequest request) {
        return service.register(request);
    }

    @GetMapping("/api/v1/auth/me")
    public UserResponse currentUser(ShopAppAuthenticationToken authentication) {
        return authentication.getProfile();
    }

    @PutMapping("/api/v1/auth/me")
    public UserResponse updateProfile(
            ShopAppAuthenticationToken authentication,
            @Valid @RequestBody UpdateProfileRequest request) {
        return settings.updateProfile(
                authentication.getToken().getSubject(),
                authentication.getProfile().getRole(),
                request);
    }

    @PostMapping("/api/v1/auth/change-password")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void changePassword(
            ShopAppAuthenticationToken authentication,
            @Valid @RequestBody ChangePasswordRequest request) {
        settings.changePassword(authentication.getToken().getSubject(), request);
    }
}
