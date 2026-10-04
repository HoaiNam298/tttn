package com.project.shopapp.exceptions;

public class InsufficientStockException extends RuntimeException {
    public InsufficientStockException(String productName, int available, int requested) {
        super(
                "Insufficient stock for %s: available %d, requested %d"
                        .formatted(productName, available, requested));
    }
}
