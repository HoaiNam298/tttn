package com.project.shopapp.model;

public record OrderLineKey(Long productId, Long variantId) implements Comparable<OrderLineKey> {
    @Override
    public int compareTo(OrderLineKey other) {
        int result = productId.compareTo(other.productId);
        if (result != 0) {
            return result;
        }
        if (variantId == null) {
            return other.variantId == null ? 0 : -1;
        }
        return other.variantId == null ? 1 : variantId.compareTo(other.variantId);
    }
}
