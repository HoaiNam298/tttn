package com.project.shopapp.services;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.project.shopapp.dtos.CreateProductReviewRequest;
import com.project.shopapp.exceptions.DuplicateResourceException;
import com.project.shopapp.exceptions.ResourceNotFoundException;
import com.project.shopapp.model.Category;
import com.project.shopapp.model.Order;
import com.project.shopapp.model.OrderItem;
import com.project.shopapp.model.OrderStatus;
import com.project.shopapp.model.Product;
import com.project.shopapp.model.ProductReview;
import com.project.shopapp.model.User;
import com.project.shopapp.repositories.OrderItemRepository;
import com.project.shopapp.repositories.ProductReviewRepository;
import com.project.shopapp.responses.ProductReviewResponse;
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
        Order order = new Order(user, "Customer", "0900000000", "HCM", "", BigDecimal.ZERO);
        order.addItem(product, 1);
        order.updateStatus(OrderStatus.CONFIRMED);
        order.updateStatus(OrderStatus.SHIPPING);
        order.updateStatus(OrderStatus.DELIVERED);
        orderItem = order.getItems().get(0);
        ReflectionTestUtils.setField(orderItem, "id", 11L);
    }

    @Test
    void createsReviewForDeliveredOwnedOrder() {
        when(orderItemRepository
                        .findByOrder_IdAndProduct_IdAndOrder_User_PhoneNumberAndOrder_Status(
                                21L, 8L, "0900000000", OrderStatus.DELIVERED))
                .thenReturn(Optional.of(orderItem));
        when(reviewRepository.existsByOrderItemId(11L)).thenReturn(false);
        when(reviewRepository.save(any(ProductReview.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        ProductReviewResponse response =
                service.create(
                        8L, "0900000000", new CreateProductReviewRequest(21L, 5, "Great product"));

        assertEquals(5, response.rating());
        assertEquals("Great product", response.comment());
    }

    @Test
    void rejectsReviewWhenDeliveredOrderDoesNotBelongToUser() {
        when(orderItemRepository
                        .findByOrder_IdAndProduct_IdAndOrder_User_PhoneNumberAndOrder_Status(
                                21L, 8L, "0999999999", OrderStatus.DELIVERED))
                .thenReturn(Optional.empty());

        assertThrows(
                ResourceNotFoundException.class,
                () ->
                        service.create(
                                8L,
                                "0999999999",
                                new CreateProductReviewRequest(21L, 5, "Fake review")));
    }

    @Test
    void rejectsDuplicateReviewForSameOrderItem() {
        when(orderItemRepository
                        .findByOrder_IdAndProduct_IdAndOrder_User_PhoneNumberAndOrder_Status(
                                21L, 8L, "0900000000", OrderStatus.DELIVERED))
                .thenReturn(Optional.of(orderItem));
        when(reviewRepository.existsByOrderItemId(11L)).thenReturn(true);

        assertThrows(
                DuplicateResourceException.class,
                () ->
                        service.create(
                                8L, "0900000000", new CreateProductReviewRequest(21L, 5, "Again")));
    }
}
