package com.project.shopapp.dtos.response;

import com.project.shopapp.model.Product;
import com.project.shopapp.model.ProductImage;
import java.math.BigDecimal;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProductDetailResponse {

    private Long id;

    private String name;

    private BigDecimal price;

    private String thumbnail;

    private String description;

    private int stock;

    private CategoryResponse category;

    private List<String> images;

    private List<ProductVariantResponse> variants;

    private long version;

    public static ProductDetailResponse from(Product product) {
        return ProductDetailResponse.builder()
                .id(product.getId())
                .name(product.getName())
                .price(product.getPrice())
                .thumbnail(product.getThumbnail())
                .description(product.getDescription())
                .stock(product.getStock())
                .category(CategoryResponse.from(product.getCategory()))
                .images(product.getImages().stream().map(ProductImage::getImageUrl).toList())
                .variants(product.getVariants().stream().map(ProductVariantResponse::from).toList())
                .version(product.getVersion())
                .build();
    }
}
