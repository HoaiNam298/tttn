package com.project.shopapp.dto;

import jakarta.validation.constraints.*;

public record LoginRequest(@NotBlank String phoneNumber, @NotBlank String password) {}
