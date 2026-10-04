package com.project.shopapp.services.impl;

import com.project.shopapp.dtos.CreateProductReviewRequest;
import com.project.shopapp.exceptions.DuplicateResourceException;
import com.project.shopapp.exceptions.ResourceNotFoundException;
import com.project.shopapp.model.OrderItem;
import com.project.shopapp.model.OrderStatus;
import com.project.shopapp.model.ProductReview;
import com.project.shopapp.repositories.OrderItemRepository;
import com.project.shopapp.repositories.ProductReviewRepository;
import com.project.shopapp.responses.ProductReviewOverviewResponse;
import com.project.shopapp.responses.ProductReviewResponse;
import com.project.shopapp.services.ProductReviewService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class ProductReviewServiceImpl implements ProductReviewService {
    private final ProductReviewRepository reviewRepository;
    private final OrderItemRepository orderItemRepository;

    public ProductReviewServiceImpl(
            ProductReviewRepository reviewRepository, OrderItemRepository orderItemRepository) {
        this.reviewRepository = reviewRepository;
        this.orderItemRepository = orderItemRepository;
    }

    @Override
    public ProductReviewOverviewResponse findByProduct(Long productId, Pageable pageable) {
        Page<ProductReviewResponse> reviews =
                reviewRepository
                        .findByProductId(productId, pageable)
                        .map(ProductReviewResponse::from);
        return new ProductReviewOverviewResponse(
                reviewRepository.averageRating(productId), reviews.getTotalElements(), reviews);
    }

    @Override
    @Transactional
    public ProductReviewResponse create(
            Long productId, String phoneNumber, CreateProductReviewRequest request) {
        OrderItem orderItem =
                orderItemRepository
                        .findByOrder_IdAndProduct_IdAndOrder_User_PhoneNumberAndOrder_Status(
                                request.orderId(), productId, phoneNumber, OrderStatus.DELIVERED)
                        .orElseThrow(
                                () ->
                                        new ResourceNotFoundException(
                                                "Delivered order item not found for this product"));
        if (reviewRepository.existsByOrderItemId(orderItem.getId())) {
            throw new DuplicateResourceException("This order item has already been reviewed");
        }
        ProductReview review =
                new ProductReview(
                        orderItem.getProduct(),
                        orderItem.getOrder().getUser(),
                        orderItem,
                        request.rating(),
                        request.comment().trim());
        return ProductReviewResponse.from(reviewRepository.save(review));
    }
}
