package com.project.shopapp.services;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

import com.project.shopapp.exceptions.ResourceNotFoundException;
import com.project.shopapp.model.Order;
import com.project.shopapp.model.OrderStatus;
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
class OrderReceiptServiceTest {
    @Mock OrderRepository orders;
    @Mock ProductRepository products;
    @Mock UserRepository users;
    private OrderService service;

    @BeforeEach
    void setUp() {
        service =
                new OrderServiceImpl(
                        orders, products, users, org.mockito.Mockito.mock(VoucherService.class));
    }

    private Order order() {
        return new Order(
                new User("Customer", "0900000000", "HCM", "password", null, null),
                "Customer",
                "0900000000",
                "HCM",
                "",
                BigDecimal.ZERO);
    }

    @Test
    void confirmsDeliveredOrderAndAllowsRetry() {
        Order order = order();
        order.updateStatus(OrderStatus.CONFIRMED);
        order.updateStatus(OrderStatus.SHIPPING);
        order.updateStatus(OrderStatus.DELIVERED);
        when(orders.findByIdAndUserPhoneNumber(21L, "0900000000")).thenReturn(Optional.of(order));
        service.confirmReceipt(21L, "0900000000");
        service.confirmReceipt(21L, "0900000000");
        assertEquals(OrderStatus.COMPLETED, order.getStatus());
    }

    @Test
    void rejectsReceiptBeforeDelivery() {
        Order order = order();
        when(orders.findByIdAndUserPhoneNumber(21L, "0900000000")).thenReturn(Optional.of(order));
        assertThrows(IllegalStateException.class, () -> service.confirmReceipt(21L, "0900000000"));
        assertEquals(OrderStatus.PENDING, order.getStatus());
    }

    @Test
    void rejectsOtherUsersOrder() {
        when(orders.findByIdAndUserPhoneNumber(21L, "0999999999")).thenReturn(Optional.empty());
        assertThrows(
                ResourceNotFoundException.class, () -> service.confirmReceipt(21L, "0999999999"));
    }

    @Test
    void preventsAdminFromCompletingOrder() {
        assertThrows(
                IllegalStateException.class,
                () -> service.updateStatus(21L, OrderStatus.COMPLETED));
    }
}
