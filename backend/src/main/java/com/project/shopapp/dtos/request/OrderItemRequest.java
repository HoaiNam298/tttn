package com.project.shopapp.dtos.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderItemRequest {

    @NotNull(message = "{validation.notnull}")
    @Min(1)
    private Long productId;

    @Min(value = 1, message = "{validation.min}")
    @Max(value = 100, message = "{validation.max}")
    private int quantity;

    @Min(1)
    private Long variantId;
}
