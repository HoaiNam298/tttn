package com.project.shopapp.responses;

import com.project.shopapp.model.Product;
import java.math.BigDecimal;
import java.util.List;

public record ProductDetailResponse(
        Long id,
        String name,
        BigDecimal price,
        String thumbnail,
        String description,
        int stock,
        CategoryResponse category,
        List<String> images) {
    public static ProductDetailResponse from(Product product) {
        return new ProductDetailResponse(
                product.getId(),
                product.getName(),
                product.getPrice(),
                product.getThumbnail(),
                product.getDescription(),
                product.getStock(),
                CategoryResponse.from(product.getCategory()),
                product.getImages().stream().map(image -> image.getImageUrl()).toList());
    }
}
