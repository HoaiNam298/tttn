package com.project.shopapp.services.impl;

import com.project.shopapp.components.MessageKeys;
import com.project.shopapp.dtos.request.RegisterRequest;
import com.project.shopapp.dtos.response.UserResponse;
import com.project.shopapp.exceptions.DuplicateResourceException;
import com.project.shopapp.exceptions.IdentityProviderException;
import com.project.shopapp.model.User;
import com.project.shopapp.repositories.UserRepository;
import com.project.shopapp.services.AuthService;
import com.project.shopapp.services.KeycloakAccounts;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;

@Service
@Slf4j
public class AuthServiceImpl implements AuthService {
    private final UserRepository users;
    private final KeycloakAccounts accounts;
    private final TransactionTemplate transaction;

    public AuthServiceImpl(
            UserRepository users, KeycloakAccounts accounts, PlatformTransactionManager manager) {
        this.users = users;
        this.accounts = accounts;
        this.transaction = new TransactionTemplate(manager);
        this.transaction.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
    }

    @Override
    public UserResponse register(RegisterRequest request) {
        if (users.existsByPhoneNumber(request.getPhoneNumber())) {
            throw new DuplicateResourceException(MessageKeys.PHONE_ALREADY_REGISTERED);
        }
        String subject = accounts.createDisabled(request);
        try {
            return transaction.execute(
                    status -> {
                        User user = saveProfile(request, subject);
                        accounts.enable(subject);
                        return UserResponse.from(user, "USER");
                    });
        } catch (RuntimeException failure) {
            deleteIncompleteAccount(subject);
            throw failure;
        }
    }

    private User saveProfile(RegisterRequest request, String subject) {
        return users.saveAndFlush(
                User.builder()
                        .keycloakUserId(subject)
                        .fullName(request.getFullName())
                        .phoneNumber(request.getPhoneNumber())
                        .address(request.getAddress())
                        .dateOfBirth(request.getDateOfBirth())
                        .build());
    }

    private void deleteIncompleteAccount(String subject) {
        try {
            accounts.delete(subject);
        } catch (RuntimeException cleanupFailure) {
            log.error("Keycloak registration compensation required subject={}", subject);
            throw new IdentityProviderException();
        }
    }
}
