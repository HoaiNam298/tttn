package com.project.shopapp.responses;

import com.project.shopapp.model.OrderItem;
import java.math.BigDecimal;

public record OrderItemResponse(
        Long productId,
        String productName,
        BigDecimal unitPrice,
        int quantity,
        BigDecimal lineTotal,
        Long variantId,
        String variantName,
        String sku) {
    public OrderItemResponse(
            Long productId,
            String productName,
            BigDecimal unitPrice,
            int quantity,
            BigDecimal lineTotal) {
        this(productId, productName, unitPrice, quantity, lineTotal, null, null, null);
    }

    public static OrderItemResponse from(OrderItem item) {
        return new OrderItemResponse(
                item.getProductId(),
                item.getProductName(),
                item.getUnitPrice(),
                item.getQuantity(),
                item.getLineTotal(),
                item.getVariantId(),
                item.getVariantName(),
                item.getSku());
    }
}
