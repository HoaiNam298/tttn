package com.project.shopapp.services;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.project.shopapp.enums.OrderStatus;
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
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AdminOrderServiceTest {
    private final OrderBusinessService orderBusinessService = new OrderBusinessServiceImpl();
    @Mock OrderRepository orderRepository;
    @Mock ProductRepository productRepository;
    @Mock UserRepository userRepository;

    private OrderService service;
    private Order order;
    private Product product;

    @BeforeEach
    void setUp() {
        service =
                new OrderServiceImpl(
                        orderRepository,
                        productRepository,
                        userRepository,
                        org.mockito.Mockito.mock(VoucherService.class),
                        null,
                        mock(OrderItemRepository.class),
                        mock(OrderStatusHistoryRepository.class),
                        new OrderBusinessServiceImpl());
        product = new Product("Phone", BigDecimal.TEN, "", "", new Category("Phone"));
        product.reserve(2);
        order =
                orderBusinessService.createOrder(
                        user(), "Customer", "0900000000", "HCM", "", BigDecimal.ZERO);
        orderBusinessService.addItem(order, product, 2);
        when(orderRepository.findWithItemsById(21L)).thenReturn(Optional.of(order));
    }

    @Test
    void cancelsPendingOrderAndRestoresStock() {
        service.updateStatus(21L, OrderStatus.CANCELLED);

        assertEquals(OrderStatus.CANCELLED, order.getStatus());
        assertEquals(100, product.getStock());
    }

    @Test
    void rejectsInvalidStatusTransition() {
        orderBusinessService.updateStatus(order, OrderStatus.CONFIRMED);
        orderBusinessService.updateStatus(order, OrderStatus.SHIPPING);

        assertThrows(
                IllegalStateException.class,
                () -> service.updateStatus(21L, OrderStatus.CANCELLED));
        assertEquals(98, product.getStock());
    }

    private User user() {
        return new User("Customer", "0900000000", "HCM", "password", null, null);
    }
}
