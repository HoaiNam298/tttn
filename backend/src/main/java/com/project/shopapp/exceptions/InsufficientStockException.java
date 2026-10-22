package com.project.shopapp.exceptions;

import com.project.shopapp.components.MessageKeys;

public class InsufficientStockException extends RuntimeException implements LocalizedError {
    private final Object[] arguments;

    public InsufficientStockException(String productName, int available, int requested) {
        super(MessageKeys.STOCK_INSUFFICIENT);
        arguments = new Object[] {productName, available, requested};
    }

    @Override
    public Object[] getArguments() {
        return arguments.clone();
    }
}
