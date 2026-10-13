package com.project.shopapp.dtos.response;

import com.project.shopapp.enums.OrderStatus;
import com.project.shopapp.enums.PaymentMethod;
import com.project.shopapp.enums.PaymentStatus;
import com.project.shopapp.enums.ShippingMethod;
import com.project.shopapp.model.Order;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderResponse {

    private Long id;

    private UUID orderNumber;

    private String recipientName;

    private String phoneNumber;

    private String shippingAddress;

    private String note;

    private OrderStatus status;

    private PaymentMethod paymentMethod;

    private BigDecimal subtotal;

    private BigDecimal shippingFee;

    private BigDecimal total;

    private Instant createdAt;

    private List<OrderItemResponse> items;

    @Builder.Default private ShippingMethod shippingMethod = ShippingMethod.STANDARD;

    private String voucherCode;

    @Builder.Default private BigDecimal discount = BigDecimal.ZERO;

    @Builder.Default private List<OrderStatusHistoryResponse> history = List.of();

    @Builder.Default private PaymentStatus paymentStatus = PaymentStatus.UNPAID;

    private Instant paymentExpiresAt;

    public static OrderResponse from(Order order) {
        return OrderResponse.builder()
                .id(order.getId())
                .orderNumber(order.getOrderNumber())
                .recipientName(order.getRecipientName())
                .phoneNumber(order.getPhoneNumber())
                .shippingAddress(order.getShippingAddress())
                .note(order.getNote())
                .status(order.getStatus())
                .paymentMethod(order.getPaymentMethod())
                .subtotal(order.getSubtotal())
                .shippingFee(order.getShippingFee())
                .total(order.getTotal())
                .createdAt(order.getCreatedAt())
                .items(order.getItems().stream().map(OrderItemResponse::from).toList())
                .shippingMethod(order.getShippingMethod())
                .voucherCode(order.getVoucherCode())
                .discount(order.getDiscount())
                .history(order.getHistory().stream().map(OrderStatusHistoryResponse::from).toList())
                .paymentStatus(order.getPaymentStatus())
                .paymentExpiresAt(order.getPaymentExpiresAt())
                .build();
    }
}
