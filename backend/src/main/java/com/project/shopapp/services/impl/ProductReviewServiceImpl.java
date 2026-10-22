package com.project.shopapp.services.impl;

import com.project.shopapp.components.MessageKeys;
import com.project.shopapp.components.TransactionLog;
import com.project.shopapp.dtos.request.CreateProductReviewRequest;
import com.project.shopapp.dtos.request.ReviewReplyRequest;
import com.project.shopapp.dtos.response.OrderReviewStatusResponse;
import com.project.shopapp.dtos.response.ProductReviewOverviewResponse;
import com.project.shopapp.dtos.response.ProductReviewResponse;
import com.project.shopapp.enums.OrderStatus;
import com.project.shopapp.exceptions.DuplicateResourceException;
import com.project.shopapp.exceptions.LocalizedArgumentException;
import com.project.shopapp.exceptions.ResourceNotFoundException;
import com.project.shopapp.model.OrderItem;
import com.project.shopapp.model.ProductReview;
import com.project.shopapp.repositories.OrderItemRepository;
import com.project.shopapp.repositories.ProductReviewRepository;
import com.project.shopapp.services.ProductReviewService;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Slf4j
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
            throw new LocalizedArgumentException(MessageKeys.REVIEW_INVALID_RATING);
        }
        pageable = bounded(pageable);
        Page<ProductReview> page =
                rating == null
                        ? reviewRepository.findByProductId(productId, pageable)
                        : reviewRepository.findByProductIdAndRating(productId, rating, pageable);
        Page<ProductReviewResponse> reviews = page.map(ProductReviewResponse::from);
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
                .orElseThrow(() -> new ResourceNotFoundException(MessageKeys.ORDER_ITEM_NOT_FOUND));
        if (reviewRepository.existsByOrderItemId(orderItem.getId())) {
            throw new DuplicateResourceException(MessageKeys.REVIEW_ALREADY_EXISTS);
        }
        ProductReview review =
                ProductReview.builder()
                        .product(orderItem.getProduct())
                        .user(orderItem.getOrder().getUser())
                        .orderItem(orderItem)
                        .rating(request.getRating())
                        .comment(request.getComment().trim())
                        .build();
        review.attachImages(images);
        ProductReview saved = reviewRepository.saveAndFlush(review);
        TransactionLog.info(
                log,
                "Product review created reviewId={} productId={} orderId={} userId={}",
                saved.getId(),
                productId,
                orderItem.getOrder().getId(),
                orderItem.getOrder().getUser().getId());
        return ProductReviewResponse.from(saved);
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
        return items.stream().map(item -> toOrderReviewStatusResponse(item, reviewedIds)).toList();
    }

    @Override
    @Transactional
    public ProductReviewResponse reply(Long reviewId, ReviewReplyRequest request) {
        ProductReview review =
                reviewRepository
                        .findById(reviewId)
                        .orElseThrow(
                                () -> new ResourceNotFoundException(MessageKeys.REVIEW_NOT_FOUND));
        review.reply(request.getReply(), request.getVersion());
        ProductReview saved = reviewRepository.saveAndFlush(review);
        TransactionLog.info(log, "Product review replied reviewId={}", reviewId);
        return ProductReviewResponse.from(saved);
    }

    private Pageable bounded(Pageable pageable) {
        return PageRequest.of(
                pageable.getPageNumber(),
                Math.min(50, pageable.getPageSize()),
                Sort.by(Sort.Direction.DESC, "createdAt", "id"));
    }

    private OrderItem eligible(Long productId, String phoneNumber, Long orderId, Long variantId) {
        Optional<OrderItem> result;
        if (variantId == null) {
            result =
                    orderItemRepository
                            .findFirstByOrder_IdAndProduct_IdAndOrder_User_PhoneNumberAndOrder_StatusOrderByIdAsc(
                                    orderId, productId, phoneNumber, OrderStatus.COMPLETED);
        } else {
            result =
                    orderItemRepository
                            .findByOrder_IdAndProduct_IdAndVariant_IdAndOrder_User_PhoneNumberAndOrder_Status(
                                    orderId,
                                    productId,
                                    variantId,
                                    phoneNumber,
                                    OrderStatus.COMPLETED);
        }
        OrderItem orderItem = result.orElse(null);
        if (orderItem == null) {
            throw new ResourceNotFoundException(MessageKeys.REVIEW_PURCHASE_REQUIRED);
        }
        return orderItem;
    }

    private OrderReviewStatusResponse toOrderReviewStatusResponse(
            OrderItem item, Set<Long> reviewedIds) {
        return OrderReviewStatusResponse.builder()
                .productId(item.getProductId())
                .variantId(item.getVariantId())
                .reviewed(reviewedIds.contains(item.getId()))
                .build();
    }
}
