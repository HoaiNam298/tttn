package com.project.shopapp.dtos;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.util.List;
import java.util.UUID;

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
        @NotEmpty(message = "Cart must contain at least one product") @Size(max = 100)
                List<@jakarta.validation.constraints.NotNull @Valid OrderItemRequest> items,
        UUID requestId,
        com.project.shopapp.model.ShippingMethod shippingMethod,
        @Size(max = 40) @Pattern(regexp = "^$|^[A-Za-z0-9_-]{3,40}$") String voucherCode,
        @jakarta.validation.constraints.DecimalMin("0")
                @jakarta.validation.constraints.Digits(integer = 12, fraction = 2)
                java.math.BigDecimal expectedTotal) {
    public CreateOrderRequest(
            String recipientName,
            String phoneNumber,
            String shippingAddress,
            String note,
            List<OrderItemRequest> items,
            UUID requestId,
            com.project.shopapp.model.ShippingMethod shippingMethod,
            String voucherCode) {
        this(
                recipientName,
                phoneNumber,
                shippingAddress,
                note,
                items,
                requestId,
                shippingMethod,
                voucherCode,
                null);
    }

    public CreateOrderRequest(
            String recipientName,
            String phoneNumber,
            String shippingAddress,
            String note,
            List<OrderItemRequest> items,
            UUID requestId) {
        this(recipientName, phoneNumber, shippingAddress, note, items, requestId, null, null);
    }

    public CreateOrderRequest(
            String recipientName,
            String phoneNumber,
            String shippingAddress,
            String note,
            List<OrderItemRequest> items) {
        this(recipientName, phoneNumber, shippingAddress, note, items, null);
    }
}
