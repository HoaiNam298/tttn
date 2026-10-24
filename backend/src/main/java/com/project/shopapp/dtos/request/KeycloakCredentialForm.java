package com.project.shopapp.dtos.request;

import org.springframework.util.LinkedMultiValueMap;

/** Preserves form serialization while hiding credentials from HTTP client debug logs. */
public class KeycloakCredentialForm extends LinkedMultiValueMap<String, String> {
    @Override
    public String toString() {
        return "KeycloakCredentialForm[redacted]";
    }
}
