package com.project.shopapp.services;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.project.shopapp.model.OrderStatus;
import com.project.shopapp.model.Product;
import com.project.shopapp.repositories.OrderRepository;
import com.project.shopapp.repositories.ProductRepository;
import com.project.shopapp.repositories.UserRepository;
import com.project.shopapp.services.impl.OrderServiceImpl;
import com.project.shopapp.services.impl.ProductServiceImpl;
import java.math.BigDecimal;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;

@ExtendWith(MockitoExtension.class)
class StorefrontFilterTest {
    @Mock ProductRepository products;
    @Mock CategoryService categories;
    @Mock OrderRepository orders;
    @Mock UserRepository users;

    @Test
    void rejectsNegativeReversedPricesAndUnknownSort() {
        ProductService service = new ProductServiceImpl(products, categories, orders);
        assertThrows(
                IllegalArgumentException.class,
                () -> service.findAll("", null, new BigDecimal("-1"), null, PageRequest.of(0, 12)));
        assertThrows(
                IllegalArgumentException.class,
                () ->
                        service.findAll(
                                "", null, BigDecimal.TEN, BigDecimal.ONE, PageRequest.of(0, 12)));
        assertThrows(
                IllegalArgumentException.class,
                () ->
                        service.findAll(
                                "", null, null, null, PageRequest.of(0, 12, Sort.by("password"))));
    }

    @Test
    void appendsStableIdSortAndCapsPageSize() {
        when(products.findAll(
                        org.mockito.ArgumentMatchers.<Specification<Product>>any(),
                        any(Pageable.class)))
                .thenReturn(Page.empty());
        new ProductServiceImpl(products, categories, orders)
                .findAll("", null, null, null, PageRequest.of(0, 500, Sort.by("price")));
        var pageable = ArgumentCaptor.forClass(Pageable.class);
        verify(products)
                .findAll(
                        org.mockito.ArgumentMatchers.<Specification<Product>>any(),
                        pageable.capture());
        assertEquals(100, pageable.getValue().getPageSize());
        assertEquals(
                Sort.Direction.DESC,
                pageable.getValue().getSort().getOrderFor("id").getDirection());
    }

    @Test
    void statusFilterAlwaysRemainsScopedToCurrentUser() {
        Pageable page = PageRequest.of(0, 10);
        when(orders.findAllByUserPhoneNumberAndStatus("0900000000", OrderStatus.SHIPPING, page))
                .thenReturn(Page.empty());
        new OrderServiceImpl(
                        orders, products, users, org.mockito.Mockito.mock(VoucherService.class))
                .findOwnedOrders("0900000000", OrderStatus.SHIPPING, page);
        verify(orders).findAllByUserPhoneNumberAndStatus("0900000000", OrderStatus.SHIPPING, page);
    }
}
