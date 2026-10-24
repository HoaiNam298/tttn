package com.project.shopapp.services;

import com.project.shopapp.dtos.request.RegisterRequest;

public interface KeycloakAccounts {
    String createDisabled(RegisterRequest request);

    void enable(String subject);

    void delete(String subject);

    void updateName(String subject, String fullName);

    void verifyPassword(String subject, String username, String password);

    void updatePassword(String subject, String password);

    void revokeSessions(String subject);
}
