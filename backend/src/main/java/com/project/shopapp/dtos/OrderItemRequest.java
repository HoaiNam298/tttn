package com.project.shopapp.dtos;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record OrderItemRequest(
        @NotNull(message = "Product is required") @Min(1) Long productId,
        @Min(value = 1, message = "Quantity must be at least 1")
                @Max(value = 100, message = "Quantity must not exceed 100")
                int quantity,
        @Min(1) Long variantId) {
    public OrderItemRequest(Long productId, int quantity) {
        this(productId, quantity, null);
    }
}
