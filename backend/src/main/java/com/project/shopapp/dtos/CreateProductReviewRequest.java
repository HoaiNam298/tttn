package com.project.shopapp.dtos;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CreateProductReviewRequest(
        @NotNull Long orderId,
        @Min(1) @Max(5) int rating,
        @NotBlank @Size(max = 1000) String comment,
        @Min(1) Long variantId) {
    public CreateProductReviewRequest(Long orderId, int rating, String comment) {
        this(orderId, rating, comment, null);
    }
}
