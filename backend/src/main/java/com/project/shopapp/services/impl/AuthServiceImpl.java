package com.project.shopapp.services.impl;

import com.project.shopapp.components.CustomUserDetailsService;
import com.project.shopapp.components.JwtService;
import com.project.shopapp.dtos.LoginRequest;
import com.project.shopapp.dtos.RegisterRequest;
import com.project.shopapp.exceptions.DuplicateResourceException;
import com.project.shopapp.exceptions.InvalidCredentialsException;
import com.project.shopapp.exceptions.ResourceNotFoundException;
import com.project.shopapp.model.Role;
import com.project.shopapp.model.Token;
import com.project.shopapp.model.User;
import com.project.shopapp.repositories.RoleRepository;
import com.project.shopapp.repositories.TokenRepository;
import com.project.shopapp.repositories.UserRepository;
import com.project.shopapp.responses.AuthResponse;
import com.project.shopapp.responses.UserResponse;
import com.project.shopapp.services.AuthService;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class AuthServiceImpl implements AuthService {
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final TokenRepository tokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final CustomUserDetailsService userDetailsService;
    private final JwtService jwtService;

    public AuthServiceImpl(
            UserRepository userRepository,
            RoleRepository roleRepository,
            TokenRepository tokenRepository,
            PasswordEncoder passwordEncoder,
            AuthenticationManager authenticationManager,
            CustomUserDetailsService userDetailsService,
            JwtService jwtService) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.tokenRepository = tokenRepository;
        this.passwordEncoder = passwordEncoder;
        this.authenticationManager = authenticationManager;
        this.userDetailsService = userDetailsService;
        this.jwtService = jwtService;
    }

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        if (userRepository.existsByPhoneNumber(request.phoneNumber())) {
            throw new DuplicateResourceException("Phone number is already registered");
        }
        Role role =
                roleRepository
                        .findByName("USER")
                        .orElseThrow(
                                () ->
                                        new ResourceNotFoundException(
                                                "Default USER role was not found"));
        User user =
                userRepository.save(
                        new User(
                                request.fullName().trim(),
                                request.phoneNumber().trim(),
                                request.address().trim(),
                                passwordEncoder.encode(request.password()),
                                request.dateOfBirth(),
                                role));
        return issueToken(user);
    }

    @Transactional
    public AuthResponse login(LoginRequest request) {
        try {
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(
                            request.phoneNumber(), request.password()));
        } catch (AuthenticationException exception) {
            throw new InvalidCredentialsException("Phone number or password is incorrect");
        }
        User user = findUser(request.phoneNumber());
        revokeActiveTokens(user.getId());
        return issueToken(user);
    }

    public UserResponse currentUser(String phoneNumber) {
        return UserResponse.from(findUser(phoneNumber));
    }

    @Transactional
    public void logout(String token) {
        tokenRepository
                .findByToken(token)
                .ifPresent(
                        saved -> {
                            saved.revoke();
                            tokenRepository.save(saved);
                        });
    }

    private AuthResponse issueToken(User user) {
        UserDetails details = userDetailsService.loadUserByUsername(user.getPhoneNumber());
        String value = jwtService.generateToken(details);
        tokenRepository.save(new Token(value, jwtService.extractExpiration(value), user));
        return new AuthResponse(
                value, "Bearer", jwtService.getExpiration() / 1000, UserResponse.from(user));
    }

    private void revokeActiveTokens(Long userId) {
        var activeTokens = tokenRepository.findAllByUserIdAndRevokedFalse(userId);
        activeTokens.forEach(Token::revoke);
        tokenRepository.saveAll(activeTokens);
    }

    private User findUser(String phoneNumber) {
        return userRepository
                .findByPhoneNumber(phoneNumber)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
    }
}
