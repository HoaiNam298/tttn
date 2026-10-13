package com.project.shopapp.services.impl;

import com.project.shopapp.dtos.CreateProductReviewRequest;
import com.project.shopapp.dtos.ReviewReplyRequest;
import com.project.shopapp.enums.OrderStatus;
import com.project.shopapp.exceptions.DuplicateResourceException;
import com.project.shopapp.exceptions.ResourceNotFoundException;
import com.project.shopapp.model.OrderItem;
import com.project.shopapp.model.ProductReview;
import com.project.shopapp.repositories.OrderItemRepository;
import com.project.shopapp.repositories.ProductReviewRepository;
import com.project.shopapp.responses.OrderReviewStatusResponse;
import com.project.shopapp.responses.ProductReviewOverviewResponse;
import com.project.shopapp.responses.ProductReviewResponse;
import com.project.shopapp.services.ProductReviewService;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
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
        return findByProduct(productId, null, pageable);
    }

    @Override
    public ProductReviewOverviewResponse findByProduct(
            Long productId, Integer rating, Pageable pageable) {
        if (rating != null && (rating < 1 || rating > 5)) {
            throw new IllegalArgumentException("Rating must be between 1 and 5");
        }
        pageable = bounded(pageable);
        Page<ProductReviewResponse> reviews =
                (rating == null
                                ? reviewRepository.findByProductId(productId, pageable)
                                : reviewRepository.findByProductIdAndRating(
                                        productId, rating, pageable))
                        .map(ProductReviewResponse::from);
        return new ProductReviewOverviewResponse(
                reviewRepository.averageRating(productId),
                reviewRepository.countByProductId(productId),
                reviews);
    }

    @Override
    @Transactional
    public ProductReviewResponse create(
            Long productId, String phoneNumber, CreateProductReviewRequest request) {
        return createWithImages(productId, phoneNumber, request, List.of());
    }

    @Override
    @Transactional
    public ProductReviewResponse createWithImages(
            Long productId,
            String phoneNumber,
            CreateProductReviewRequest request,
            List<String> images) {
        OrderItem orderItem =
                eligible(productId, phoneNumber, request.getOrderId(), request.getVariantId());
        orderItemRepository
                .lockById(orderItem.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Order item not found"));
        if (reviewRepository.existsByOrderItemId(orderItem.getId())) {
            throw new DuplicateResourceException("This order item has already been reviewed");
        }
        ProductReview review =
                new ProductReview(
                        orderItem.getProduct(),
                        orderItem.getOrder().getUser(),
                        orderItem,
                        request.getRating(),
                        request.getComment().trim());
        review.attachImages(images);
        return ProductReviewResponse.from(reviewRepository.saveAndFlush(review));
    }

    @Override
    public boolean reviewed(Long productId, String phoneNumber, Long orderId, Long variantId) {
        return reviewRepository.existsByOrderItemId(
                eligible(productId, phoneNumber, orderId, variantId).getId());
    }

    @Override
    public Page<ProductReviewResponse> findAll(Pageable pageable) {
        return reviewRepository.findAll(bounded(pageable)).map(ProductReviewResponse::from);
    }

    @Override
    public List<OrderReviewStatusResponse> orderReviewStatuses(String phoneNumber, Long orderId) {
        var items =
                orderItemRepository.findByOrderIdAndOrderUserPhoneNumberAndOrderStatus(
                        orderId, phoneNumber, OrderStatus.COMPLETED);
        var reviewedIds = reviewRepository.reviewedItemIds(orderId, phoneNumber);
        return items.stream()
                .map(
                        item ->
                                new OrderReviewStatusResponse(
                                        item.getProductId(),
                                        item.getVariantId(),
                                        reviewedIds.contains(item.getId())))
                .toList();
    }

    @Override
    @Transactional
    public ProductReviewResponse reply(Long reviewId, ReviewReplyRequest request) {
        ProductReview review =
                reviewRepository
                        .findById(reviewId)
                        .orElseThrow(() -> new ResourceNotFoundException("Review not found"));
        review.reply(request.getReply(), request.getVersion());
        return ProductReviewResponse.from(reviewRepository.saveAndFlush(review));
    }

    private Pageable bounded(Pageable pageable) {
        return PageRequest.of(
                pageable.getPageNumber(),
                Math.min(50, pageable.getPageSize()),
                Sort.by(Sort.Direction.DESC, "createdAt", "id"));
    }

    private OrderItem eligible(Long productId, String phoneNumber, Long orderId, Long variantId) {
        OrderItem orderItem =
                (variantId == null
                                ? orderItemRepository
                                        .findFirstByOrder_IdAndProduct_IdAndOrder_User_PhoneNumberAndOrder_StatusOrderByIdAsc(
                                                orderId,
                                                productId,
                                                phoneNumber,
                                                OrderStatus.COMPLETED)
                                : orderItemRepository
                                        .findByOrder_IdAndProduct_IdAndVariant_IdAndOrder_User_PhoneNumberAndOrder_Status(
                                                orderId,
                                                productId,
                                                variantId,
                                                phoneNumber,
                                                OrderStatus.COMPLETED))
                        .orElseThrow(
                                () ->
                                        new ResourceNotFoundException(
                                                "Completed order item not found for this product"));
        return orderItem;
    }
}
