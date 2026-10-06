package com.project.shopapp.dtos;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record AddressRequest(
        @NotBlank @Size(max = 100) String recipientName,
        @NotBlank @Pattern(regexp = "^[0-9+]{9,15}$") String phoneNumber,
        @NotBlank @Size(max = 255) String shippingAddress,
        boolean defaultAddress) {}
