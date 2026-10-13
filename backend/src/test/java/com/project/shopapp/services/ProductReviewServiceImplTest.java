package com.project.shopapp.services;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.project.shopapp.dtos.request.CreateProductReviewRequest;
import com.project.shopapp.dtos.response.ProductReviewResponse;
import com.project.shopapp.enums.OrderStatus;
import com.project.shopapp.exceptions.DuplicateResourceException;
import com.project.shopapp.exceptions.ResourceNotFoundException;
import com.project.shopapp.model.Category;
import com.project.shopapp.model.Order;
import com.project.shopapp.model.OrderItem;
import com.project.shopapp.model.Product;
import com.project.shopapp.model.ProductReview;
import com.project.shopapp.model.User;
import com.project.shopapp.repositories.OrderItemRepository;
import com.project.shopapp.repositories.ProductReviewRepository;
import com.project.shopapp.services.impl.OrderBusinessServiceImpl;
import com.project.shopapp.services.impl.ProductReviewServiceImpl;
import java.math.BigDecimal;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class ProductReviewServiceImplTest {
    private final OrderBusinessService orderBusinessService = new OrderBusinessServiceImpl();
    @Mock ProductReviewRepository reviewRepository;
    @Mock OrderItemRepository orderItemRepository;

    private ProductReviewService service;
    private OrderItem orderItem;

    @BeforeEach
    void setUp() {
        service = new ProductReviewServiceImpl(reviewRepository, orderItemRepository);
        Product product = new Product("Phone", BigDecimal.TEN, "", "", new Category("Phone"));
        ReflectionTestUtils.setField(product, "id", 8L);
        User user = new User("Customer", "0900000000", "HCM", "password", null, null);
        Order order =
                orderBusinessService.createOrder(
                        user, "Customer", "0900000000", "HCM", "", BigDecimal.ZERO);
        orderBusinessService.addItem(order, product, 1);
        orderBusinessService.updateStatus(order, OrderStatus.CONFIRMED);
        orderBusinessService.updateStatus(order, OrderStatus.SHIPPING);
        orderBusinessService.updateStatus(order, OrderStatus.DELIVERED);
        orderBusinessService.updateStatus(order, OrderStatus.COMPLETED);
        orderItem = order.getItems().get(0);
        ReflectionTestUtils.setField(orderItem, "id", 11L);
    }

    @Test
    void createsReviewForCompletedOwnedOrder() {
        when(orderItemRepository
                        .findFirstByOrder_IdAndProduct_IdAndOrder_User_PhoneNumberAndOrder_StatusOrderByIdAsc(
                                21L, 8L, "0900000000", OrderStatus.COMPLETED))
                .thenReturn(Optional.of(orderItem));
        when(reviewRepository.existsByOrderItemId(11L)).thenReturn(false);
        when(orderItemRepository.lockById(11L)).thenReturn(Optional.of(orderItem));
        when(reviewRepository.saveAndFlush(any(ProductReview.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        ProductReviewResponse response =
                service.create(
                        8L,
                        "0900000000",
                        CreateProductReviewRequest.builder()
                                .orderId(21L)
                                .rating(5)
                                .comment("Great product")
                                .build());

        assertEquals(5, response.getRating());
        assertEquals("Great product", response.getComment());
    }

    @Test
    void rejectsReviewWhenCompletedOrderDoesNotBelongToUser() {
        when(orderItemRepository
                        .findFirstByOrder_IdAndProduct_IdAndOrder_User_PhoneNumberAndOrder_StatusOrderByIdAsc(
                                21L, 8L, "0999999999", OrderStatus.COMPLETED))
                .thenReturn(Optional.empty());

        assertThrows(
                ResourceNotFoundException.class,
                () ->
                        service.create(
                                8L,
                                "0999999999",
                                CreateProductReviewRequest.builder()
                                        .orderId(21L)
                                        .rating(5)
                                        .comment("Fake review")
                                        .build()));
    }

    @Test
    void rejectsDuplicateReviewForSameOrderItem() {
        when(orderItemRepository
                        .findFirstByOrder_IdAndProduct_IdAndOrder_User_PhoneNumberAndOrder_StatusOrderByIdAsc(
                                21L, 8L, "0900000000", OrderStatus.COMPLETED))
                .thenReturn(Optional.of(orderItem));
        when(reviewRepository.existsByOrderItemId(11L)).thenReturn(true);
        when(orderItemRepository.lockById(11L)).thenReturn(Optional.of(orderItem));

        assertThrows(
                DuplicateResourceException.class,
                () ->
                        service.create(
                                8L,
                                "0900000000",
                                CreateProductReviewRequest.builder()
                                        .orderId(21L)
                                        .rating(5)
                                        .comment("Again")
                                        .build()));
    }

    @Test
    void filteredPagePreservesGlobalCountAndAverage() {
        when(reviewRepository.findByProductIdAndRating(
                        org.mockito.ArgumentMatchers.eq(8L),
                        org.mockito.ArgumentMatchers.eq(4),
                        any()))
                .thenReturn(org.springframework.data.domain.Page.empty());
        when(reviewRepository.averageRating(8L)).thenReturn(4.2);
        when(reviewRepository.countByProductId(8L)).thenReturn(10L);
        var response =
                service.findByProduct(
                        8L, 4, org.springframework.data.domain.PageRequest.of(0, 500));
        assertEquals(10, response.getTotalReviews());
        assertEquals(4.2, response.getAverageRating());
    }

    @Test
    void rejectsInvalidRatingFilter() {
        assertThrows(
                IllegalArgumentException.class,
                () ->
                        service.findByProduct(
                                8L, 6, org.springframework.data.domain.PageRequest.of(0, 10)));
    }

    @Test
    void staleReplyDoesNotOverwriteCurrentResponse() {
        ProductReview review =
                new ProductReview(
                        orderItem.getProduct(),
                        orderItem.getOrder().getUser(),
                        orderItem,
                        5,
                        "Great");
        ReflectionTestUtils.setField(review, "version", 2L);
        when(reviewRepository.findById(1L)).thenReturn(Optional.of(review));
        assertThrows(
                IllegalStateException.class,
                () ->
                        service.reply(
                                1L,
                                new com.project.shopapp.dtos.request.ReviewReplyRequest(
                                        "Thank you", 1L)));
        assertEquals(null, review.getShopReply());
    }
}
