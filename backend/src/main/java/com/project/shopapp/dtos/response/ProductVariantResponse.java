package com.project.shopapp.dtos.response;

import com.project.shopapp.model.ProductVariant;
import java.math.BigDecimal;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProductVariantResponse {

    private Long id;

    private String sku;

    private String name;

    private String color;

    private String size;

    private String capacity;

    private BigDecimal price;

    private int stock;

    private String imageUrl;

    private boolean active;

    public static ProductVariantResponse from(ProductVariant variant) {
        return ProductVariantResponse.builder()
                .id(variant.getId())
                .sku(variant.getSku())
                .name(variant.getName())
                .color(variant.getColor())
                .size(variant.getSize())
                .capacity(variant.getCapacity())
                .price(variant.getPrice())
                .stock(variant.getStock())
                .imageUrl(variant.getImageUrl())
                .active(variant.isActive())
                .build();
    }
}
