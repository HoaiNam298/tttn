package com.project.shopapp.dto;

import java.math.BigDecimal;
import java.time.Instant;
import com.project.shopapp.model.Product;

public record ProductResponse(
        Long id, String name, BigDecimal price, String thumbnail,
        String description, CategoryResponse category,
        Instant createdAt, Instant updatedAt) {
    public static ProductResponse from(Product product) {
        return new ProductResponse(product.getId(), product.getName(), product.getPrice(),
                product.getThumbnail(), product.getDescription(),
                CategoryResponse.from(product.getCategory()),
                product.getCreatedAt(), product.getUpdatedAt());
    }
}
