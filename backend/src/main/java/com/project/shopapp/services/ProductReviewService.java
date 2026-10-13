package com.project.shopapp.services;

import com.project.shopapp.dtos.CreateProductReviewRequest;
import com.project.shopapp.dtos.ReviewReplyRequest;
import com.project.shopapp.responses.OrderReviewStatusResponse;
import com.project.shopapp.responses.ProductReviewOverviewResponse;
import com.project.shopapp.responses.ProductReviewResponse;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface ProductReviewService {
    ProductReviewOverviewResponse findByProduct(Long productId, Pageable pageable);

    ProductReviewOverviewResponse findByProduct(Long productId, Integer rating, Pageable pageable);

    boolean reviewed(Long productId, String phoneNumber, Long orderId, Long variantId);

    List<OrderReviewStatusResponse> orderReviewStatuses(String phoneNumber, Long orderId);

    ProductReviewResponse createWithImages(
            Long productId,
            String phoneNumber,
            CreateProductReviewRequest request,
            List<String> images);

    Page<ProductReviewResponse> findAll(Pageable pageable);

    ProductReviewResponse reply(Long reviewId, ReviewReplyRequest request);

    ProductReviewResponse create(
            Long productId, String phoneNumber, CreateProductReviewRequest request);
}
