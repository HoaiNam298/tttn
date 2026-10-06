package com.project.shopapp.dtos;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;

public record ProductRequest(
        @NotBlank @Size(max = 350) String name,
        @NotNull @DecimalMin(value = "0.0", inclusive = true) @Digits(integer = 10, fraction = 2)
                BigDecimal price,
        @Size(max = 500)
                @Pattern(regexp = "^(https?://[^\\s]+|/api/v1/media/[a-f0-9-]+\\.(png|jpg))?$")
                String thumbnail,
        @Size(max = 10000) String description,
        @NotNull @Positive Long categoryId) {}
