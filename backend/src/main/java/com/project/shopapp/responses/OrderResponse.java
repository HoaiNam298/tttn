package com.project.shopapp.responses;

import com.project.shopapp.enums.OrderStatus;
import com.project.shopapp.enums.PaymentMethod;
import com.project.shopapp.enums.PaymentStatus;
import com.project.shopapp.enums.ShippingMethod;
import com.project.shopapp.model.Order;
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
        ShippingMethod shippingMethod,
        String voucherCode,
        BigDecimal discount,
        List<OrderStatusHistoryResponse> history,
        PaymentStatus paymentStatus,
        Instant paymentExpiresAt) {
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
                ShippingMethod.STANDARD,
                null,
                BigDecimal.ZERO,
                List.of(),
                PaymentStatus.UNPAID,
                null);
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
                order.getHistory().stream().map(OrderStatusHistoryResponse::from).toList(),
                order.getPaymentStatus(),
                order.getPaymentExpiresAt());
    }
}
