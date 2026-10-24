package com.project.shopapp.services.impl;

import com.project.shopapp.components.MessageKeys;
import com.project.shopapp.dtos.request.ChangePasswordRequest;
import com.project.shopapp.dtos.request.UpdateProfileRequest;
import com.project.shopapp.dtos.response.UserResponse;
import com.project.shopapp.exceptions.IdentityProviderException;
import com.project.shopapp.exceptions.LocalizedArgumentException;
import com.project.shopapp.exceptions.LocalizedStateException;
import com.project.shopapp.exceptions.ResourceNotFoundException;
import com.project.shopapp.model.User;
import com.project.shopapp.repositories.UserRepository;
import com.project.shopapp.services.AccountSettingsService;
import com.project.shopapp.services.KeycloakAccounts;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;

@Service
@Slf4j
public class AccountSettingsServiceImpl implements AccountSettingsService {
    private final UserRepository users;
    private final KeycloakAccounts accounts;
    private final TransactionTemplate transaction;

    public AccountSettingsServiceImpl(
            UserRepository users, KeycloakAccounts accounts, PlatformTransactionManager manager) {
        this.users = users;
        this.accounts = accounts;
        this.transaction = new TransactionTemplate(manager);
        this.transaction.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
    }

    @Override
    public UserResponse updateProfile(String subject, String role, UpdateProfileRequest request) {
        // Commit the source of truth and the retry marker together, before remote I/O.
        Long id =
                transaction.execute(
                        status -> {
                            User user = lock(subject);
                            user.setFullName(request.getFullName().strip());
                            user.setAddress(request.getAddress().strip());
                            user.setDateOfBirth(request.getDateOfBirth());
                            user.setProfileSyncPending(true);
                            users.saveAndFlush(user);
                            return user.getId();
                        });
        synchronize(id);
        return UserResponse.from(users.findById(id).orElseThrow(), role);
    }

    @Override
    public void changePassword(String subject, ChangePasswordRequest request) {
        if (request.getCurrentPassword().equals(request.getNewPassword())) {
            throw new LocalizedArgumentException(MessageKeys.PASSWORD_UNCHANGED);
        }
        boolean[] verificationUnavailable = {false};
        String operationId = java.util.UUID.randomUUID().toString();
        Long id =
                transaction.execute(
                        status -> {
                            User user = lock(subject);
                            if (user.isSessionRevocationPending()) {
                                throw new LocalizedStateException(
                                        MessageKeys.PASSWORD_CHANGE_PENDING);
                            }
                            // Serialize verification as well as reset to reject simultaneous
                            // old-password use.
                            try {
                                accounts.verifyPassword(
                                        subject,
                                        user.getPhoneNumber(),
                                        request.getCurrentPassword());
                            } catch (IdentityProviderException exception) {
                                // The grant or its logout may have timed out after creating a
                                // temporary session. Persist cleanup without resetting a password.
                                verificationUnavailable[0] = true;
                            }
                            user.setSessionRevocationPending(true);
                            user.setPasswordChangeId(operationId);
                            users.saveAndFlush(user);
                            return user.getId();
                        });
        if (verificationUnavailable[0]) {
            throw new LocalizedStateException(MessageKeys.PASSWORD_CHANGE_PENDING);
        }
        // The durable marker survives a crash, timeout or failed DB commit after reset.
        // The worker uses the same row lock. If it already processed the marker, abort.
        try {
            transaction.executeWithoutResult(
                    status -> {
                        User user = users.findForUpdateById(id).orElseThrow();
                        if (!user.isSessionRevocationPending()
                                || !operationId.equals(user.getPasswordChangeId())) {
                            throw new LocalizedStateException(MessageKeys.PASSWORD_CHANGE_PENDING);
                        }
                        accounts.updatePassword(subject, request.getNewPassword());
                        try {
                            accounts.revokeSessions(subject);
                            user.setSessionRevocationPending(false);
                            user.setPasswordChangeId(null);
                            users.saveAndFlush(user);
                        } catch (IdentityProviderException exception) {
                            log.warn("Session revocation queued userId={}", id);
                        }
                    });
        } catch (LocalizedArgumentException exception) {
            // Known policy rejection; the queued revocation is still safe to retry.
            throw exception;
        } catch (RuntimeException exception) {
            log.warn("Password change outcome requires sign-in verification userId={}", id);
            throw new LocalizedStateException(MessageKeys.PASSWORD_CHANGE_UNCERTAIN);
        }
        if (users.findById(id).orElseThrow().isSessionRevocationPending()) {
            throw new LocalizedStateException(MessageKeys.PASSWORD_CHANGED_LOGOUT_PENDING);
        }
    }

    private User lock(String subject) {
        User user =
                users.findByKeycloakUserId(subject)
                        .orElseThrow(
                                () -> new ResourceNotFoundException(MessageKeys.USER_NOT_FOUND));
        return users.findForUpdateById(user.getId()).orElseThrow();
    }

    private void synchronize(Long id) {
        // Independent transactions prevent a name-sync failure from rolling back revocation.
        for (boolean revocation : List.of(true, false)) {
            try {
                transaction.executeWithoutResult(
                        status -> {
                            User user = users.findForUpdateById(id).orElse(null);
                            if (user == null) {
                                return;
                            }
                            if (revocation && user.isSessionRevocationPending()) {
                                accounts.revokeSessions(user.getKeycloakUserId());
                                user.setSessionRevocationPending(false);
                                user.setPasswordChangeId(null);
                                users.saveAndFlush(user);
                            } else if (!revocation && user.isProfileSyncPending()) {
                                accounts.updateName(user.getKeycloakUserId(), user.getFullName());
                                user.setProfileSyncPending(false);
                                users.saveAndFlush(user);
                            }
                        });
            } catch (RuntimeException exception) {
                log.warn(
                        "Identity synchronization pending userId={} revocation={}", id, revocation);
            }
        }
    }

    @Override
    public void synchronizePending() {
        long afterId = 0;
        while (true) {
            List<Long> batch = users.findIdentitySyncPendingAfter(afterId, PageRequest.of(0, 50));
            if (batch.isEmpty()) {
                return;
            }
            batch.forEach(this::synchronize);
            afterId = batch.get(batch.size() - 1);
        }
    }
}
