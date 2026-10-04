package com.project.shopapp.services;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.project.shopapp.dtos.CreateOrderRequest;
import com.project.shopapp.dtos.OrderItemRequest;
import com.project.shopapp.exceptions.InsufficientStockException;
import com.project.shopapp.model.Category;
import com.project.shopapp.model.Order;
import com.project.shopapp.model.Product;
import com.project.shopapp.model.User;
import com.project.shopapp.repositories.OrderRepository;
import com.project.shopapp.repositories.ProductRepository;
import com.project.shopapp.repositories.UserRepository;
import com.project.shopapp.responses.OrderResponse;
import com.project.shopapp.services.impl.OrderServiceImpl;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class OrderServiceImplTest {
    @Mock OrderRepository orderRepository;
    @Mock ProductRepository productRepository;
    @Mock UserRepository userRepository;

    private OrderService service;
    private Product product;

    @BeforeEach
    void setUp() {
        service = new OrderServiceImpl(orderRepository, productRepository, userRepository);
        product = new Product("Phone", new BigDecimal("100000"), "", "", new Category("Phone"));
        ReflectionTestUtils.setField(product, "id", 8L);
        when(userRepository.findByPhoneNumber("0900000000")).thenReturn(Optional.of(user()));
        when(productRepository.findAllByIdInOrderById(any())).thenReturn(List.of(product));
    }

    @Test
    void calculatesPricesOnServerAndAggregatesDuplicateItems() {
        when(orderRepository.save(any(Order.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        OrderResponse response =
                service.create(
                        "0900000000",
                        request(List.of(new OrderItemRequest(8L, 1), new OrderItemRequest(8L, 2))));

        assertEquals(new BigDecimal("300000"), response.subtotal());
        assertEquals(new BigDecimal("330000.00"), response.total());
        assertEquals(3, response.items().get(0).quantity());
    }

    @Test
    void rejectsQuantityAboveStock() {
        assertThrows(
                InsufficientStockException.class,
                () ->
                        service.create(
                                "0900000000", request(List.of(new OrderItemRequest(8L, 101)))));
    }

    private CreateOrderRequest request(List<OrderItemRequest> items) {
        return new CreateOrderRequest("Nguyen Van A", "0900000000", "1 Nguyen Hue, HCM", "", items);
    }

    private User user() {
        return new User("Nguyen Van A", "0900000000", "HCM", "password", null, null);
    }
}
