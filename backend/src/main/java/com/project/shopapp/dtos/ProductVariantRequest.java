package com.project.shopapp.dtos;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;

public record ProductVariantRequest(
        Long id,
        @NotBlank @Size(max = 64) @Pattern(regexp = "^[A-Za-z0-9_-]+$") String sku,
        @NotBlank @Size(max = 150) String name,
        @Size(max = 60) String color,
        @Size(max = 60) String size,
        @Size(max = 60) String capacity,
        @NotNull @DecimalMin("0") @Digits(integer = 10, fraction = 2) BigDecimal price,
        @Min(0) @Max(1000000) int stock,
        @Size(max = 500)
                @Pattern(regexp = "^(https?://[^\\s]+|/api/v1/media/[a-f0-9-]+\\.(png|jpg))?$")
                String imageUrl,
        boolean active) {}
