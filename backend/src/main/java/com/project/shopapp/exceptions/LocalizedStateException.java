package com.project.shopapp.exceptions;

public class LocalizedStateException extends IllegalStateException implements LocalizedError {
    private final Object[] arguments;

    public LocalizedStateException(String key, Object... arguments) {
        super(key);
        this.arguments = arguments.clone();
    }

    public LocalizedStateException(String key, Throwable cause) {
        super(key, cause);
        this.arguments = new Object[0];
    }

    @Override
    public Object[] getArguments() {
        return arguments.clone();
    }
}
