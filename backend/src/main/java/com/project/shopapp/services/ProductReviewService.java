package com.project.shopapp.services;

import com.project.shopapp.dtos.CreateProductReviewRequest;
import com.project.shopapp.responses.ProductReviewOverviewResponse;
import com.project.shopapp.responses.ProductReviewResponse;
import org.springframework.data.domain.Pageable;

public interface ProductReviewService {
    ProductReviewOverviewResponse findByProduct(Long productId, Pageable pageable);

    ProductReviewOverviewResponse findByProduct(Long productId, Integer rating, Pageable pageable);

    boolean reviewed(Long productId, String phoneNumber, Long orderId, Long variantId);

    java.util.List<com.project.shopapp.responses.OrderReviewStatusResponse> orderReviewStatuses(
            String phoneNumber, Long orderId);

    ProductReviewResponse createWithImages(
            Long productId,
            String phoneNumber,
            CreateProductReviewRequest request,
            java.util.List<String> images);

    org.springframework.data.domain.Page<ProductReviewResponse> findAll(Pageable pageable);

    ProductReviewResponse reply(Long reviewId, com.project.shopapp.dtos.ReviewReplyRequest request);

    ProductReviewResponse create(
            Long productId, String phoneNumber, CreateProductReviewRequest request);
}
