package com.project.shopapp.responses;

import com.project.shopapp.model.ProductVariant;
import java.math.BigDecimal;

public record ProductVariantResponse(
        Long id,
        String sku,
        String name,
        String color,
        String size,
        String capacity,
        BigDecimal price,
        int stock,
        String imageUrl,
        boolean active) {
    public static ProductVariantResponse from(ProductVariant variant) {
        return new ProductVariantResponse(
                variant.getId(),
                variant.getSku(),
                variant.getName(),
                variant.getColor(),
                variant.getSize(),
                variant.getCapacity(),
                variant.getPrice(),
                variant.getStock(),
                variant.getImageUrl(),
                variant.isActive());
    }
}
