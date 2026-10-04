package com.project.shopapp.services;

import com.project.shopapp.dtos.CreateProductReviewRequest;
import com.project.shopapp.responses.ProductReviewOverviewResponse;
import com.project.shopapp.responses.ProductReviewResponse;
import org.springframework.data.domain.Pageable;

public interface ProductReviewService {
    ProductReviewOverviewResponse findByProduct(Long productId, Pageable pageable);

    ProductReviewResponse create(
            Long productId, String phoneNumber, CreateProductReviewRequest request);
}
