package com.project.shopapp.services.impl;

import com.project.shopapp.components.MessageKeys;
import com.project.shopapp.enums.OrderStatus;
import com.project.shopapp.enums.PaymentMethod;
import com.project.shopapp.enums.PaymentStatus;
import com.project.shopapp.enums.ShippingMethod;
import com.project.shopapp.exceptions.LocalizedStateException;
import com.project.shopapp.model.Order;
import com.project.shopapp.model.OrderItem;
import com.project.shopapp.model.OrderStatusHistory;
import com.project.shopapp.model.Product;
import com.project.shopapp.model.ProductVariant;
import com.project.shopapp.model.User;
import com.project.shopapp.model.Voucher;
import com.project.shopapp.services.OrderBusinessService;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;
import org.springframework.stereotype.Service;

@Service
public class OrderBusinessServiceImpl implements OrderBusinessService {

    @Override
    public Order createOrder(
            User user,
            String recipientName,
            String phoneNumber,
            String shippingAddress,
            String note,
            BigDecimal shippingFee) {
        Order order =
                Order.builder()
                        .orderNumber(UUID.randomUUID())
                        .user(user)
                        .recipientName(recipientName)
                        .phoneNumber(phoneNumber)
                        .shippingAddress(shippingAddress)
                        .note(note)
                        .status(OrderStatus.PENDING)
                        .paymentMethod(PaymentMethod.COD)
                        .subtotal(BigDecimal.ZERO)
                        .shippingFee(shippingFee)
                        .total(shippingFee)
                        .build();
        addHistory(order, "CUSTOMER");
        return order;
    }

    @Override
    public void addItem(Order order, Product product, ProductVariant variant, int quantity) {
        BigDecimal unitPrice = variant == null ? product.getPrice() : variant.getPrice();
        BigDecimal lineTotal = unitPrice.multiply(BigDecimal.valueOf(quantity));
        OrderItem item =
                OrderItem.builder()
                        .order(order)
                        .product(product)
                        .productName(product.getName())
                        .variant(variant)
                        .variantName(variant == null ? null : variant.getName())
                        .sku(variant == null ? null : variant.getSku())
                        .unitPrice(unitPrice)
                        .quantity(quantity)
                        .lineTotal(lineTotal)
                        .build();
        order.getItems().add(item);
        order.setSubtotal(order.getSubtotal().add(lineTotal));
        updateTotal(order);
    }

    @Override
    public void selectShipping(Order order, ShippingMethod method) {
        order.setShippingMethod(method);
        order.setShippingFee(method.getFee());
        updateTotal(order);
    }

    @Override
    public void applyVoucher(Order order, Voucher voucher) {
        order.setVoucher(voucher);
        order.setVoucherCode(voucher.getCode());
        order.setDiscount(voucher.calculateDiscount(order.getSubtotal()));
        updateTotal(order);
    }

    @Override
    public void selectOnlinePayment(Order order, Instant expiresAt) {
        order.setPaymentMethod(PaymentMethod.VNPAY);
        order.setPaymentStatus(PaymentStatus.PENDING);
        order.setPaymentExpiresAt(expiresAt);
    }

    @Override
    public void recordPayment(Order order, PaymentStatus result) {
        if (order.getPaymentMethod() != PaymentMethod.VNPAY) {
            throw new LocalizedStateException(MessageKeys.ORDER_NOT_ONLINE);
        }
        order.setPaymentStatus(result);
    }

    @Override
    public OrderStatusHistory updateStatus(Order order, OrderStatus nextStatus, String actor) {
        if (order.getPaymentMethod() == PaymentMethod.VNPAY) {
            if (nextStatus == OrderStatus.CANCELLED
                    && (order.getPaymentStatus() == PaymentStatus.PAID
                            || order.getPaymentStatus() == PaymentStatus.REVIEW_REQUIRED)) {
                throw new LocalizedStateException(MessageKeys.ORDER_REFUND_REQUIRED);
            }
            if (nextStatus == OrderStatus.CONFIRMED
                    && order.getPaymentStatus() != PaymentStatus.PAID) {
                throw new LocalizedStateException(MessageKeys.ORDER_PAYMENT_UNVERIFIED);
            }
        }
        if (!order.getStatus().canTransitionTo(nextStatus)) {
            throw new LocalizedStateException(
                    MessageKeys.ORDER_INVALID_TRANSITION, order.getStatus(), nextStatus);
        }
        order.setStatus(nextStatus);
        if (nextStatus == OrderStatus.CANCELLED
                && order.getPaymentStatus() == PaymentStatus.PENDING) {
            order.setPaymentStatus(PaymentStatus.CANCELLED);
        }
        if (nextStatus == OrderStatus.COMPLETED) {
            if (order.getPaymentMethod() == PaymentMethod.COD) {
                order.setPaymentStatus(PaymentStatus.PAID);
            }
            order.setCompletedAt(Instant.now());
            order.setCompletionTimeEstimated(false);
        }
        return addHistory(order, actor);
    }

    private void updateTotal(Order order) {
        order.setTotal(
                order.getSubtotal().add(order.getShippingFee()).subtract(order.getDiscount()));
    }

    private OrderStatusHistory addHistory(Order order, String actor) {
        OrderStatusHistory history =
                OrderStatusHistory.builder()
                        .order(order)
                        .status(order.getStatus())
                        .actor(actor)
                        .occurredAt(Instant.now())
                        .build();
        order.getHistory().add(history);
        return history;
    }
}
