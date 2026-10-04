package com.project.shopapp.dtos;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.util.List;

public record CreateOrderRequest(
        @NotBlank(message = "Recipient name is required")
                @Size(max = 100, message = "Recipient name must not exceed 100 characters")
                String recipientName,
        @NotBlank(message = "Phone number is required")
                @Pattern(regexp = "^[0-9+]{9,15}$", message = "Phone number is invalid")
                String phoneNumber,
        @NotBlank(message = "Shipping address is required")
                @Size(max = 255, message = "Shipping address must not exceed 255 characters")
                String shippingAddress,
        @Size(max = 500, message = "Note must not exceed 500 characters") String note,
        @NotEmpty(message = "Cart must contain at least one product")
                List<@Valid OrderItemRequest> items) {}
