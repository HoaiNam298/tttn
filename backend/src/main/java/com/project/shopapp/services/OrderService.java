package com.project.shopapp.services;

import com.project.shopapp.dtos.CreateOrderRequest;
import com.project.shopapp.responses.OrderResponse;

public interface OrderService {
    OrderResponse create(String phoneNumber, CreateOrderRequest request);

    OrderResponse findOwnedOrder(Long id, String phoneNumber);
}
