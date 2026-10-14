package com.project.shopapp.services;

import com.project.shopapp.dtos.request.RegisterRequest;

public interface KeycloakAccounts {
    String createDisabled(RegisterRequest request);

    void enable(String subject);

    void delete(String subject);
}
