package com.project.shopapp.services;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.project.shopapp.dtos.CreateOrderRequest;
import com.project.shopapp.dtos.OrderItemRequest;
import com.project.shopapp.dtos.VoucherRequest;
import com.project.shopapp.enums.OrderStatus;
import com.project.shopapp.enums.ShippingMethod;
import com.project.shopapp.enums.VoucherType;
import com.project.shopapp.exceptions.ResourceNotFoundException;
import com.project.shopapp.model.Category;
import com.project.shopapp.model.Order;
import com.project.shopapp.model.Product;
import com.project.shopapp.model.User;
import com.project.shopapp.model.Voucher;
import com.project.shopapp.repositories.OrderItemRepository;
import com.project.shopapp.repositories.OrderRepository;
import com.project.shopapp.repositories.OrderStatusHistoryRepository;
import com.project.shopapp.repositories.ProductRepository;
import com.project.shopapp.repositories.UserRepository;
import com.project.shopapp.services.impl.OrderBusinessServiceImpl;
import com.project.shopapp.services.impl.OrderServiceImpl;
import java.math.BigDecimal;
import java.time.Instant;
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
class OrderOperationsTest {
    private final OrderBusinessService orderBusinessService = new OrderBusinessServiceImpl();
    @Mock OrderRepository orders;
    @Mock ProductRepository products;
    @Mock UserRepository users;
    @Mock VoucherService vouchers;
    private OrderService service;
    private User user;
    private Product product;

    @BeforeEach
    void setUp() {
        service =
                new OrderServiceImpl(
                        orders,
                        products,
                        users,
                        vouchers,
                        null,
                        mock(OrderItemRepository.class),
                        mock(OrderStatusHistoryRepository.class),
                        new OrderBusinessServiceImpl());
        user = new User("Customer", "0900000000", "HCM", "pw", null, null);
        ReflectionTestUtils.setField(user, "id", 1L);
        product = new Product("Phone", new BigDecimal("100000"), "", "", new Category("Phones"));
        ReflectionTestUtils.setField(product, "id", 8L);
    }

    private CreateOrderRequest request(UUID key, ShippingMethod method, String code) {
        return CreateOrderRequest.builder()
                .recipientName("Customer")
                .phoneNumber("0900000000")
                .shippingAddress("HCM")
                .note("")
                .items(List.of(OrderItemRequest.builder().productId(8L).quantity(1).build()))
                .requestId(key)
                .shippingMethod(method)
                .voucherCode(code)
                .build();
    }

    private Voucher voucher() {
        Voucher voucher =
                new Voucher(
                        new VoucherRequest(
                                "SAVE10",
                                VoucherType.FIXED,
                                new BigDecimal("10000"),
                                BigDecimal.ZERO,
                                new BigDecimal("10000"),
                                Instant.now().minusSeconds(60),
                                Instant.now().plusSeconds(60),
                                1,
                                1,
                                null,
                                true,
                                null));
        ReflectionTestUtils.setField(voucher, "id", 4L);
        return voucher;
    }

    @Test
    void quoteCalculatesShippingAndDiscountWithoutReservingAnything() {
        when(users.findByPhoneNumber("0900000000")).thenReturn(Optional.of(user));
        when(products.findAllById(any())).thenReturn(List.of(product));
        when(vouchers.preview("SAVE10", 1L, new BigDecimal("100000")))
                .thenReturn(new BigDecimal("10000"));
        var quote = service.quote("0900000000", request(null, ShippingMethod.EXPRESS, "SAVE10"));
        assertEquals(new BigDecimal("140000.00"), quote.total());
        assertEquals(100, product.getStock());
        verify(vouchers, never()).reserve(any(), any(), any());
        verify(orders, never()).save(any());
    }

    @Test
    void sameRequestRetriesOnceButShippingOrVoucherChangeConflicts() {
        UUID key = UUID.randomUUID();
        when(users.findForUpdateByPhoneNumber("0900000000")).thenReturn(Optional.of(user));
        when(products.findAllByIdInOrderById(any())).thenReturn(List.of(product));
        when(vouchers.reserve(eq("SAVE10"), eq(1L), any())).thenReturn(voucher());
        when(orders.save(any()))
                .thenAnswer(
                        call -> {
                            Order saved = call.getArgument(0);
                            when(orders.findByUserIdAndRequestId(1L, key))
                                    .thenReturn(Optional.of(saved));
                            return saved;
                        });
        var input = request(key, ShippingMethod.EXPRESS, "SAVE10");
        service.create("0900000000", input);
        var retry = service.create("0900000000", input);
        assertEquals(new BigDecimal("140000.00"), retry.total());
        assertEquals(99, product.getStock());
        verify(vouchers).reserve(eq("SAVE10"), eq(1L), any());
        assertThrows(
                IllegalStateException.class,
                () ->
                        service.create(
                                "0900000000", request(key, ShippingMethod.STANDARD, "SAVE10")));
        assertThrows(
                IllegalStateException.class,
                () -> service.create("0900000000", request(key, ShippingMethod.EXPRESS, "OTHER")));
    }

    @Test
    void ownedCancelRestoresStockAndVoucherOnceAndKeepsAuditTrail() {
        Order order =
                orderBusinessService.createOrder(
                        user, "Customer", "0900000000", "HCM", "", BigDecimal.ZERO);
        product.reserve(1);
        orderBusinessService.addItem(order, product, 1);
        orderBusinessService.applyVoucher(order, voucher());
        when(orders.findByIdAndUserPhoneNumber(21L, "0900000000")).thenReturn(Optional.of(order));
        when(products.findAllByIdInOrderById(any())).thenReturn(List.of(product));
        service.cancelOwnedOrder(21L, "0900000000");
        service.cancelOwnedOrder(21L, "0900000000");
        assertEquals(100, product.getStock());
        assertEquals(2, order.getHistory().size());
        assertEquals("CUSTOMER", order.getHistory().get(1).getActor());
        verify(vouchers).release(4L);
    }

    @Test
    void refusesToSaveWhenConfirmedQuoteTotalHasChanged() {
        when(users.findByPhoneNumber("0900000000")).thenReturn(Optional.of(user));
        when(products.findAllByIdInOrderById(any())).thenReturn(List.of(product));
        when(vouchers.reserve(eq("SAVE10"), eq(1L), any())).thenReturn(voucher());
        var input =
                CreateOrderRequest.builder()
                        .recipientName("Customer")
                        .phoneNumber("0900000000")
                        .shippingAddress("HCM")
                        .note("")
                        .items(
                                List.of(
                                        OrderItemRequest.builder()
                                                .productId(8L)
                                                .quantity(1)
                                                .build()))
                        .shippingMethod(ShippingMethod.EXPRESS)
                        .voucherCode("SAVE10")
                        .expectedTotal(new BigDecimal("150000"))
                        .build();
        assertThrows(IllegalStateException.class, () -> service.create("0900000000", input));
        verify(orders, never()).save(any());
    }

    @Test
    void cannotCancelConfirmedOrOtherUsersOrder() {
        Order order =
                orderBusinessService.createOrder(
                        user, "Customer", "0900000000", "HCM", "", BigDecimal.ZERO);
        orderBusinessService.updateStatus(order, OrderStatus.CONFIRMED);
        when(orders.findByIdAndUserPhoneNumber(21L, "0900000000")).thenReturn(Optional.of(order));
        assertThrows(
                IllegalStateException.class, () -> service.cancelOwnedOrder(21L, "0900000000"));
        when(orders.findByIdAndUserPhoneNumber(21L, "0999999999")).thenReturn(Optional.empty());
        assertThrows(
                ResourceNotFoundException.class, () -> service.cancelOwnedOrder(21L, "0999999999"));
        verify(vouchers, never()).release(any());
    }

    @Test
    void expiryReleasesStockAndVoucherOnlyOnce() {
        Order order =
                orderBusinessService.createOrder(
                        user, "Customer", "0900000000", "HCM", "", BigDecimal.ZERO);
        product.reserve(1);
        orderBusinessService.addItem(order, product, 1);
        orderBusinessService.applyVoucher(order, voucher());
        orderBusinessService.selectOnlinePayment(order, Instant.now().minusSeconds(5));
        when(orders.findWithItemsById(21L)).thenReturn(Optional.of(order));
        when(products.findAllByIdInOrderById(any())).thenReturn(List.of(product));
        service.closeUnpaidPayment(21L, com.project.shopapp.enums.PaymentStatus.EXPIRED);
        service.closeUnpaidPayment(21L, com.project.shopapp.enums.PaymentStatus.EXPIRED);
        assertEquals(100, product.getStock());
        verify(vouchers).release(4L);
        assertEquals(com.project.shopapp.enums.PaymentStatus.EXPIRED, order.getPaymentStatus());
        assertEquals(OrderStatus.CANCELLED, order.getStatus());
    }
}
