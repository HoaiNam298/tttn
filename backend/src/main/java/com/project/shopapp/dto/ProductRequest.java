package com.project.shopapp.dto;

import java.math.BigDecimal;
import jakarta.validation.constraints.*;

public record ProductRequest(
        @NotBlank @Size(max = 350) String name,
        @NotNull @DecimalMin(value = "0.0", inclusive = true) BigDecimal price,
        @Size(max = 500) String thumbnail,
        @Size(max = 10000) String description,
        @NotNull @Positive Long categoryId) {}
