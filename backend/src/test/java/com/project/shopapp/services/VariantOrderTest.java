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
import com.project.shopapp.model.ProductVariant;
import com.project.shopapp.model.User;
import com.project.shopapp.repositories.OrderRepository;
import com.project.shopapp.repositories.ProductRepository;
import com.project.shopapp.repositories.UserRepository;
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
class VariantOrderTest {
    @Mock OrderRepository orders;
    @Mock ProductRepository products;
    @Mock UserRepository users;
    private Product product;
    private ProductVariant black;
    private ProductVariant white;
    private OrderService service;

    @BeforeEach
    void setUp() {
        product = new Product("Phone", BigDecimal.TEN, "", "", new Category("Phone"));
        ReflectionTestUtils.setField(product, "id", 8L);
        black = variant(1L, "BLACK", "Black 128GB", 100, 3);
        white = variant(2L, "WHITE", "White 256GB", 200, 2);
        product.addVariant(black);
        product.addVariant(white);
        product.refreshVariantSummary();
        service =
                new OrderServiceImpl(
                        orders, products, users, org.mockito.Mockito.mock(VoucherService.class));
        when(users.findByPhoneNumber("0900000000"))
                .thenReturn(
                        Optional.of(new User("Customer", "0900000000", "HCM", "pw", null, null)));
        when(products.findAllByIdInOrderById(any())).thenReturn(List.of(product));
    }

    private ProductVariant variant(Long id, String sku, String name, int price, int stock) {
        var variant = new ProductVariant(product);
        variant.update(sku, name, "", "", "", BigDecimal.valueOf(price), stock, "", true);
        ReflectionTestUtils.setField(variant, "id", id);
        return variant;
    }

    private CreateOrderRequest request(OrderItemRequest... lines) {
        return new CreateOrderRequest("Customer", "0900000000", "HCM", "", List.of(lines));
    }

    @Test
    void separatesVariantsAndSnapshotsPriceNameAndSku() {
        when(orders.save(any(Order.class))).thenAnswer(call -> call.getArgument(0));
        var result =
                service.create(
                        "0900000000",
                        request(new OrderItemRequest(8L, 2, 1L), new OrderItemRequest(8L, 1, 2L)));
        assertEquals(2, result.items().size());
        assertEquals(new BigDecimal("400"), result.subtotal());
        assertEquals("BLACK", result.items().get(0).sku());
        assertEquals("Black 128GB", result.items().get(0).variantName());
        assertEquals(1, black.getStock());
        assertEquals(1, white.getStock());
        assertEquals(2, product.getStock());
        black.update("NEW", "Renamed", "", "", "", BigDecimal.valueOf(999), 1, "", true);
        assertEquals(BigDecimal.valueOf(100), result.items().get(0).unitPrice());
        assertEquals("BLACK", result.items().get(0).sku());
    }

    @Test
    void requiresVariantAndRejectsForeignVariant() {
        assertThrows(
                IllegalStateException.class,
                () -> service.create("0900000000", request(new OrderItemRequest(8L, 1))));
        assertThrows(
                IllegalArgumentException.class,
                () -> service.create("0900000000", request(new OrderItemRequest(8L, 1, 999L))));
    }

    @Test
    void rejectsInsufficientVariantStock() {
        assertThrows(
                InsufficientStockException.class,
                () -> service.create("0900000000", request(new OrderItemRequest(8L, 4, 1L))));
        assertEquals(3, black.getStock());
    }
}
