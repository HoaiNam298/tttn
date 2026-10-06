package com.project.shopapp.services;

import com.project.shopapp.dtos.CreateOrderRequest;
import com.project.shopapp.model.OrderStatus;
import com.project.shopapp.responses.OrderResponse;
import com.project.shopapp.responses.OrderSummaryResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface OrderService {
    OrderResponse create(String phoneNumber, CreateOrderRequest request);

    com.project.shopapp.responses.CheckoutQuoteResponse quote(
            String phoneNumber, CreateOrderRequest request);

    OrderResponse cancelOwnedOrder(Long id, String phoneNumber);

    OrderResponse findOwnedOrder(Long id, String phoneNumber);

    Page<OrderSummaryResponse> findOwnedOrders(String phoneNumber, Pageable pageable);

    Page<OrderSummaryResponse> findOwnedOrders(
            String phoneNumber, OrderStatus status, Pageable pageable);

    Page<OrderSummaryResponse> findAll(OrderStatus status, Pageable pageable);

    OrderResponse updateStatus(Long id, OrderStatus status);

    OrderResponse confirmReceipt(Long id, String phoneNumber);

    OrderResponse findAdminOrder(Long id);
}
