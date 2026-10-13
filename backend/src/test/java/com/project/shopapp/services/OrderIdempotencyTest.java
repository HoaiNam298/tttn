package com.project.shopapp.services;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.project.shopapp.dtos.CreateOrderRequest;
import com.project.shopapp.dtos.OrderItemRequest;
import com.project.shopapp.model.Category;
import com.project.shopapp.model.Order;
import com.project.shopapp.model.Product;
import com.project.shopapp.model.User;
import com.project.shopapp.repositories.OrderItemRepository;
import com.project.shopapp.repositories.OrderRepository;
import com.project.shopapp.repositories.OrderStatusHistoryRepository;
import com.project.shopapp.repositories.ProductRepository;
import com.project.shopapp.repositories.UserRepository;
import com.project.shopapp.services.impl.OrderBusinessServiceImpl;
import com.project.shopapp.services.impl.OrderServiceImpl;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class OrderIdempotencyTest {
    private final OrderBusinessService orderBusinessService = new OrderBusinessServiceImpl();
    @Mock OrderRepository orders;
    @Mock ProductRepository products;
    @Mock UserRepository users;
    private OrderService service;
    private User user;
    private final UUID requestId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        service =
                new OrderServiceImpl(
                        orders,
                        products,
                        users,
                        org.mockito.Mockito.mock(VoucherService.class),
                        null,
                        mock(OrderItemRepository.class),
                        mock(OrderStatusHistoryRepository.class),
                        new OrderBusinessServiceImpl());
        user = new User("Customer", "0900000000", "HCM", "password", null, null);
        ReflectionTestUtils.setField(user, "id", 1L);
        when(users.findForUpdateByPhoneNumber("0900000000")).thenReturn(Optional.of(user));
    }

    private CreateOrderRequest request(String address) {
        return CreateOrderRequest.builder()
                .recipientName("Customer")
                .phoneNumber("0900000000")
                .shippingAddress(address)
                .note("")
                .items(List.of(OrderItemRequest.builder().productId(8L).quantity(2).build()))
                .requestId(requestId)
                .build();
    }

    @Test
    void retryReturnsSameOrderAndReservesStockOnlyOnce() {
        Product product = new Product("Phone", BigDecimal.TEN, "", "", new Category("Phone"));
        ReflectionTestUtils.setField(product, "id", 8L);
        when(products.findAllByIdInOrderById(any())).thenReturn(List.of(product));
        when(orders.findByUserIdAndRequestId(1L, requestId)).thenReturn(Optional.empty());
        when(orders.save(any(Order.class)))
                .thenAnswer(
                        invocation -> {
                            Order saved = invocation.getArgument(0);
                            ReflectionTestUtils.setField(saved, "id", 21L);
                            when(orders.findByUserIdAndRequestId(1L, requestId))
                                    .thenReturn(Optional.of(saved));
                            return saved;
                        });
        assertEquals(21L, service.create("0900000000", request("HCM")).id());
        assertEquals(21L, service.create("0900000000", request("HCM")).id());
        assertEquals(98, product.getStock());
        verify(orders).save(any(Order.class));
    }

    @Test
    void rejectsReusingKeyForDifferentPayload() {
        Order existing =
                orderBusinessService.createOrder(
                        user, "Customer", "0900000000", "HCM", "", BigDecimal.ZERO);
        existing.setRequestId(requestId);
        existing.setRequestFingerprint("different fingerprint");
        when(orders.findByUserIdAndRequestId(1L, requestId)).thenReturn(Optional.of(existing));
        assertThrows(
                IllegalStateException.class,
                () -> service.create("0900000000", request("New address")));
        verify(products, never()).findAllByIdInOrderById(any());
    }
}
