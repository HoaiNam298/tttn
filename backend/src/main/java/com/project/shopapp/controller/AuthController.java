package com.project.shopapp.controller;

import com.project.shopapp.dtos.LoginRequest;
import com.project.shopapp.dtos.RegisterRequest;
import com.project.shopapp.exceptions.InvalidCredentialsException;
import com.project.shopapp.responses.AuthResponse;
import com.project.shopapp.responses.UserResponse;
import com.project.shopapp.services.AuthService;
import jakarta.validation.Valid;
import java.security.Principal;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {
    private final AuthService service;

    public AuthController(AuthService service) {
        this.service = service;
    }

    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(@Valid @RequestBody RegisterRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.register(request));
    }

    @PostMapping("/login")
    public AuthResponse login(@Valid @RequestBody LoginRequest request) {
        return service.login(request);
    }

    @GetMapping("/me")
    public UserResponse currentUser(Principal principal) {
        return service.currentUser(principal.getName());
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(@RequestHeader("Authorization") String authorization) {
        if (!authorization.startsWith("Bearer ") || authorization.length() <= 7) {
            throw new InvalidCredentialsException("Authorization header is invalid");
        }
        service.logout(authorization.substring(7));
        return ResponseEntity.noContent().build();
    }
}
