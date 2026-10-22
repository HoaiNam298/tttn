package com.project.shopapp.exceptions;

public class DuplicateResourceException extends RuntimeException implements LocalizedError {
    private final Object[] arguments;

    public DuplicateResourceException(String key, Object... arguments) {
        super(key);
        this.arguments = arguments.clone();
    }

    public DuplicateResourceException(String key, Throwable cause) {
        super(key, cause);
        this.arguments = new Object[0];
    }

    @Override
    public Object[] getArguments() {
        return arguments.clone();
    }
}
