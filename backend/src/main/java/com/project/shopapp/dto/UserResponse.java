package com.project.shopapp.dto;

import com.project.shopapp.model.User;
import java.time.LocalDate;

public record UserResponse(
        Long id,
        String fullName,
        String phoneNumber,
        String address,
        LocalDate dateOfBirth,
        String role) {
    public static UserResponse from(User user) {
        return new UserResponse(
                user.getId(),
                user.getFullName(),
                user.getPhoneNumber(),
                user.getAddress(),
                user.getDateOfBirth(),
                user.getRole().getName());
    }
}
