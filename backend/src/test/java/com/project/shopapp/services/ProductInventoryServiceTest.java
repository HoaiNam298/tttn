package com.project.shopapp.services;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import com.project.shopapp.dtos.ProductInventoryRequest;
import com.project.shopapp.dtos.ProductVariantRequest;
import com.project.shopapp.model.Category;
import com.project.shopapp.model.Product;
import com.project.shopapp.repositories.OrderRepository;
import com.project.shopapp.repositories.ProductRepository;
import com.project.shopapp.services.impl.ProductServiceImpl;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class ProductInventoryServiceTest {
    @Mock ProductRepository products;
    @Mock CategoryService categories;
    @Mock OrderRepository orders;

    @Test
    void staleInventoryIsRejectedBeforeStockIsOverwritten() {
        Product product = new Product("Phone", BigDecimal.TEN, "", "", new Category("Phone"));
        ReflectionTestUtils.setField(product, "version", 3L);
        when(products.findForUpdateById(8L)).thenReturn(Optional.of(product));
        var service = new ProductServiceImpl(products, categories, orders);
        assertThrows(
                IllegalStateException.class,
                () ->
                        service.updateInventory(
                                8L, new ProductInventoryRequest(2L, 999, List.of(), List.of())));
    }

    @Test
    void convertingLegacyProductWithOpenOrdersIsRejected() {
        Product product = new Product("Phone", BigDecimal.TEN, "", "", new Category("Phone"));
        when(products.findForUpdateById(8L)).thenReturn(Optional.of(product));
        when(orders.existsOpenLegacyItems(eq(8L), any())).thenReturn(true);
        var variant =
                new ProductVariantRequest(
                        null, "BLACK", "Black", "", "", "", BigDecimal.TEN, 10, "", true);
        assertThrows(
                IllegalStateException.class,
                () ->
                        new ProductServiceImpl(products, categories, orders)
                                .updateInventory(
                                        8L,
                                        new ProductInventoryRequest(
                                                0L, null, List.of(variant), List.of())));
    }
}
