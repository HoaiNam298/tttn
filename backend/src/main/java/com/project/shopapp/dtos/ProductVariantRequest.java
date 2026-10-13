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
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProductVariantRequest {

    private Long id;

    @NotBlank
    @Size(max = 64)
    @Pattern(regexp = "^[A-Za-z0-9_-]+$")
    private String sku;

    @NotBlank
    @Size(max = 150)
    private String name;

    @Size(max = 60)
    private String color;

    @Size(max = 60)
    private String size;

    @Size(max = 60)
    private String capacity;

    @NotNull
    @DecimalMin("0")
    @Digits(integer = 10, fraction = 2)
    private BigDecimal price;

    @Min(0)
    @Max(1000000)
    private int stock;

    @Size(max = 500)
    @Pattern(regexp = "^(https?://[^\\s]+|/api/v1/media/[a-f0-9-]+\\.(png|jpg))?$")
    private String imageUrl;

    private boolean active;
}
