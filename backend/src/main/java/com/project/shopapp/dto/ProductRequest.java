package com.project.shopapp.dto;

import jakarta.validation.constraints.*;
import java.math.BigDecimal;

public record ProductRequest(
        @NotBlank @Size(max = 350) String name,
        @NotNull @DecimalMin(value = "0.0", inclusive = true) BigDecimal price,
        @Size(max = 500) String thumbnail,
        @Size(max = 10000) String description,
        @NotNull @Positive Long categoryId) {}
