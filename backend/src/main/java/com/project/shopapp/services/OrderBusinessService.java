package com.project.shopapp.services;

import com.project.shopapp.enums.OrderStatus;
import com.project.shopapp.enums.PaymentStatus;
import com.project.shopapp.enums.ShippingMethod;
import com.project.shopapp.model.Order;
import com.project.shopapp.model.OrderStatusHistory;
import com.project.shopapp.model.Product;
import com.project.shopapp.model.ProductVariant;
import com.project.shopapp.model.User;
import com.project.shopapp.model.Voucher;
import java.math.BigDecimal;
import java.time.Instant;

public interface OrderBusinessService {

    Order createOrder(
            User user,
            String recipientName,
            String phoneNumber,
            String shippingAddress,
            String note,
            BigDecimal shippingFee);

    void addItem(Order order, Product product, ProductVariant variant, int quantity);

    void selectShipping(Order order, ShippingMethod method);

    void applyVoucher(Order order, Voucher voucher);

    void selectOnlinePayment(Order order, Instant expiresAt);

    void recordPayment(Order order, PaymentStatus result);

    OrderStatusHistory updateStatus(Order order, OrderStatus nextStatus, String actor);
}
