package com.project.shopapp.responses;

import com.project.shopapp.model.Order;
import com.project.shopapp.model.OrderStatus;
import com.project.shopapp.model.PaymentMethod;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record OrderResponse(
        Long id,
        UUID orderNumber,
        String recipientName,
        String phoneNumber,
        String shippingAddress,
        String note,
        OrderStatus status,
        PaymentMethod paymentMethod,
        BigDecimal subtotal,
        BigDecimal shippingFee,
        BigDecimal total,
        Instant createdAt,
        List<OrderItemResponse> items,
        com.project.shopapp.model.ShippingMethod shippingMethod,
        String voucherCode,
        BigDecimal discount,
        List<OrderStatusHistoryResponse> history) {
    public OrderResponse(
            Long id,
            UUID orderNumber,
            String recipientName,
            String phoneNumber,
            String shippingAddress,
            String note,
            OrderStatus status,
            PaymentMethod paymentMethod,
            BigDecimal subtotal,
            BigDecimal shippingFee,
            BigDecimal total,
            Instant createdAt,
            List<OrderItemResponse> items) {
        this(
                id,
                orderNumber,
                recipientName,
                phoneNumber,
                shippingAddress,
                note,
                status,
                paymentMethod,
                subtotal,
                shippingFee,
                total,
                createdAt,
                items,
                com.project.shopapp.model.ShippingMethod.STANDARD,
                null,
                BigDecimal.ZERO,
                List.of());
    }

    public static OrderResponse from(Order order) {
        return new OrderResponse(
                order.getId(),
                order.getOrderNumber(),
                order.getRecipientName(),
                order.getPhoneNumber(),
                order.getShippingAddress(),
                order.getNote(),
                order.getStatus(),
                order.getPaymentMethod(),
                order.getSubtotal(),
                order.getShippingFee(),
                order.getTotal(),
                order.getCreatedAt(),
                order.getItems().stream().map(OrderItemResponse::from).toList(),
                order.getShippingMethod(),
                order.getVoucherCode(),
                order.getDiscount(),
                order.getHistory().stream().map(OrderStatusHistoryResponse::from).toList());
    }
}
