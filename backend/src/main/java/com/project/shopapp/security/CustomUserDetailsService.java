package com.project.shopapp.security;

import com.project.shopapp.model.User;
import com.project.shopapp.repository.UserRepository;
import org.springframework.security.core.userdetails.*;
import org.springframework.stereotype.Service;

@Service
public class CustomUserDetailsService implements UserDetailsService {
    private final UserRepository repository;

    public CustomUserDetailsService(UserRepository repository) {
        this.repository = repository;
    }

    @Override
    public UserDetails loadUserByUsername(String phoneNumber) throws UsernameNotFoundException {
        User user =
                repository
                        .findByPhoneNumber(phoneNumber)
                        .orElseThrow(() -> new UsernameNotFoundException("User not found"));
        return org.springframework.security.core.userdetails.User.withUsername(
                        user.getPhoneNumber())
                .password(user.getPassword())
                .roles(user.getRole().getName())
                .disabled(!user.isActive())
                .build();
    }
}
