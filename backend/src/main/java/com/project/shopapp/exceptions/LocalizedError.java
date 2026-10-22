package com.project.shopapp.exceptions;

public interface LocalizedError {
    String getMessage();

    Object[] getArguments();
}
