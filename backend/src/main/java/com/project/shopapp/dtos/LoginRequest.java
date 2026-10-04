package com.project.shopapp.dtos;

import jakarta.validation.constraints.NotBlank;

public record LoginRequest(@NotBlank String phoneNumber, @NotBlank String password) {}
