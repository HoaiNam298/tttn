package com.project.shopapp.responses;

import com.project.shopapp.model.Order;
import com.project.shopapp.model.OrderStatus;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record OrderSummaryResponse(
        Long id,
        UUID orderNumber,
        String recipientName,
        String phoneNumber,
        OrderStatus status,
        BigDecimal total,
        Instant createdAt) {
    public static OrderSummaryResponse from(Order order) {
        return new OrderSummaryResponse(
                order.getId(),
                order.getOrderNumber(),
                order.getRecipientName(),
                order.getPhoneNumber(),
                order.getStatus(),
                order.getTotal(),
                order.getCreatedAt());
    }
}
