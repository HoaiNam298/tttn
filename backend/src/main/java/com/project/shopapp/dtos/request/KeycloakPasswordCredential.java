package com.project.shopapp.dtos.request;

public record KeycloakPasswordCredential(String type, String value, boolean temporary) {
    @Override
    public String toString() {
        return "KeycloakPasswordCredential[redacted]";
    }
}
