package com.project.shopapp.dtos.request;

import com.project.shopapp.enums.PaymentMethod;
import com.project.shopapp.enums.ShippingMethod;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateOrderRequest {

    @NotBlank(message = "Recipient name is required")
    @Size(max = 100, message = "Recipient name must not exceed 100 characters")
    private String recipientName;

    @NotBlank(message = "Phone number is required")
    @Pattern(regexp = "^[0-9+]{9,15}$", message = "Phone number is invalid")
    private String phoneNumber;

    @NotBlank(message = "Shipping address is required")
    @Size(max = 255, message = "Shipping address must not exceed 255 characters")
    private String shippingAddress;

    @Size(max = 500, message = "Note must not exceed 500 characters")
    private String note;

    @NotEmpty(message = "Cart must contain at least one product")
    @Size(max = 100)
    private List<@NotNull @Valid OrderItemRequest> items;

    private UUID requestId;

    private ShippingMethod shippingMethod;

    @Size(max = 40)
    @Pattern(regexp = "^$|^[A-Za-z0-9_-]{3,40}$")
    private String voucherCode;

    @DecimalMin("0")
    @Digits(integer = 12, fraction = 2)
    private BigDecimal expectedTotal;

    private PaymentMethod paymentMethod;
}
