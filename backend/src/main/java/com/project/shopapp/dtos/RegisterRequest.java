package com.project.shopapp.dtos;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Past;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;

public record RegisterRequest(
        @NotBlank @Size(max = 100) String fullName,
        @NotBlank
                @Pattern(
                        regexp = "^[0-9]{9,15}$",
                        message = "Phone number must contain 9 to 15 digits")
                String phoneNumber,
        @NotBlank @Size(min = 8, max = 72) String password,
        @NotBlank @Size(max = 255) String address,
        @Past LocalDate dateOfBirth) {}
