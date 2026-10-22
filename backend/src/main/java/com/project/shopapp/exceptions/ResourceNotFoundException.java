package com.project.shopapp.exceptions;

public class ResourceNotFoundException extends RuntimeException implements LocalizedError {
    private final Object[] arguments;

    public ResourceNotFoundException(String key, Object... arguments) {
        super(key);
        this.arguments = arguments.clone();
    }

    public ResourceNotFoundException(String key, Throwable cause) {
        super(key, cause);
        this.arguments = new Object[0];
    }

    @Override
    public Object[] getArguments() {
        return arguments.clone();
    }
}
