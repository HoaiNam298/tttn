package com.project.shopapp.exceptions;

import com.project.shopapp.components.MessageKeys;

public class IdentityProviderException extends RuntimeException implements LocalizedError {
    public IdentityProviderException() {
        super(MessageKeys.IDENTITY_PROVIDER_UNAVAILABLE);
    }

    @Override
    public Object[] getArguments() {
        return new Object[0];
    }
}
