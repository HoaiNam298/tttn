package com.project.shopapp.services;

import com.project.shopapp.dtos.ProductRequest;
import com.project.shopapp.responses.ProductResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface ProductService {
    Page<ProductResponse> findAll(String keyword, Long categoryId, Pageable pageable);

    ProductResponse findById(Long id);

    ProductResponse create(ProductRequest request);

    ProductResponse update(Long id, ProductRequest request);

    void delete(Long id);
}
