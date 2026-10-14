package com.project.shopapp.exceptions;

public class IdentityProviderException extends RuntimeException {
    public IdentityProviderException() {
        super("Account registration is temporarily unavailable");
    }
}
