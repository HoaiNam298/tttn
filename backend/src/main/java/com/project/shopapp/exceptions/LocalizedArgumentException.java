package com.project.shopapp.exceptions;

public class LocalizedArgumentException extends IllegalArgumentException implements LocalizedError {
    private final Object[] arguments;

    public LocalizedArgumentException(String key, Object... arguments) {
        super(key);
        this.arguments = arguments.clone();
    }

    public LocalizedArgumentException(String key, Throwable cause) {
        super(key, cause);
        this.arguments = new Object[0];
    }

    @Override
    public Object[] getArguments() {
        return arguments.clone();
    }
}
