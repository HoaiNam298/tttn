package com.project.shopapp.services;

import com.project.shopapp.dtos.ProductRequest;
import com.project.shopapp.responses.ProductDetailResponse;
import com.project.shopapp.responses.ProductResponse;
import java.math.BigDecimal;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface ProductService {
    Page<ProductResponse> findAll(String keyword, Long categoryId, Pageable pageable);

    Page<ProductResponse> findAll(
            String keyword,
            Long categoryId,
            BigDecimal minPrice,
            BigDecimal maxPrice,
            Pageable pageable);

    ProductDetailResponse findById(Long id);

    ProductDetailResponse updateInventory(
            Long id, com.project.shopapp.dtos.ProductInventoryRequest request);

    ProductResponse create(ProductRequest request);

    ProductResponse update(Long id, ProductRequest request);

    void delete(Long id);
}
