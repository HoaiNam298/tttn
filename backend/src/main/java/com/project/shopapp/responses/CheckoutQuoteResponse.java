package com.project.shopapp.responses;

import com.project.shopapp.model.ShippingMethod;
import java.math.BigDecimal;

public record CheckoutQuoteResponse(
        BigDecimal subtotal,
        ShippingMethod shippingMethod,
        BigDecimal shippingFee,
        String voucherCode,
        BigDecimal discount,
        BigDecimal total) {}
