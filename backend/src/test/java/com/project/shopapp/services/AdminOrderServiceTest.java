package com.project.shopapp.services;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

import com.project.shopapp.model.Category;
import com.project.shopapp.model.Order;
import com.project.shopapp.model.OrderStatus;
import com.project.shopapp.model.Product;
import com.project.shopapp.model.User;
import com.project.shopapp.repositories.OrderRepository;
import com.project.shopapp.repositories.ProductRepository;
import com.project.shopapp.repositories.UserRepository;
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
    @Mock OrderRepository orderRepository;
    @Mock ProductRepository productRepository;
    @Mock UserRepository userRepository;

    private OrderService service;
    private Order order;
    private Product product;

    @BeforeEach
    void setUp() {
        service = new OrderServiceImpl(orderRepository, productRepository, userRepository);
        product = new Product("Phone", BigDecimal.TEN, "", "", new Category("Phone"));
        product.reserve(2);
        order = new Order(user(), "Customer", "0900000000", "HCM", "", BigDecimal.ZERO);
        order.addItem(product, 2);
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
        order.updateStatus(OrderStatus.CONFIRMED);
        order.updateStatus(OrderStatus.SHIPPING);

        assertThrows(
                IllegalStateException.class,
                () -> service.updateStatus(21L, OrderStatus.CANCELLED));
        assertEquals(98, product.getStock());
    }

    private User user() {
        return new User("Customer", "0900000000", "HCM", "password", null, null);
    }
}
