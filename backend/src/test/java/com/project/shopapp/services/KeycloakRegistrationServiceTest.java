package com.project.shopapp.services;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.project.shopapp.dtos.request.RegisterRequest;
import com.project.shopapp.exceptions.DuplicateResourceException;
import com.project.shopapp.exceptions.IdentityProviderException;
import com.project.shopapp.model.User;
import com.project.shopapp.repositories.UserRepository;
import com.project.shopapp.services.impl.AuthServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionStatus;

class KeycloakRegistrationServiceTest {
    private final UserRepository users = mock(UserRepository.class);
    private final KeycloakAccounts accounts = mock(KeycloakAccounts.class);
    private final PlatformTransactionManager manager = mock(PlatformTransactionManager.class);
    private final TransactionStatus status = mock(TransactionStatus.class);
    private final RegisterRequest request =
            RegisterRequest.builder()
                    .fullName("Test User")
                    .phoneNumber("0912345678")
                    .password("test-password")
                    .address("Test address")
                    .build();
    private AuthService service;

    @BeforeEach
    void setup() {
        when(manager.getTransaction(any())).thenReturn(status);
        when(accounts.createDisabled(request)).thenReturn("subject");
        when(users.saveAndFlush(any()))
                .thenAnswer(
                        invocation -> {
                            User user = invocation.getArgument(0);
                            user.setId(42L);
                            return user;
                        });
        service = new AuthServiceImpl(users, accounts, manager);
    }

    @Test
    void savesProfileWithoutLocalPasswordOrRoleAndCommits() {
        var response = service.register(request);
        assertEquals("USER", response.getRole());
        var saved = ArgumentCaptor.forClass(User.class);
        verify(users).saveAndFlush(saved.capture());
        assertEquals("subject", saved.getValue().getKeycloakUserId());
        verify(accounts).enable("subject");
        verify(manager).commit(status);
        verify(accounts, never()).delete(any());
    }

    @Test
    void duplicateLocalPhoneDoesNotCreateRemoteUser() {
        when(users.existsByPhoneNumber(request.getPhoneNumber())).thenReturn(true);
        assertThrows(DuplicateResourceException.class, () -> service.register(request));
        verify(accounts, never()).createDisabled(any());
    }

    @Test
    void failedSaveRollsBackAndDeletesRemoteUser() {
        doThrow(new IllegalStateException("save failed")).when(users).saveAndFlush(any());
        assertThrows(IllegalStateException.class, () -> service.register(request));
        verify(manager).rollback(status);
        verify(accounts).delete("subject");
        verify(accounts, never()).enable(any());
    }

    @Test
    void failedCommitAlsoDeletesRemoteUser() {
        doThrow(new IllegalStateException("commit failed")).when(manager).commit(status);
        assertThrows(IllegalStateException.class, () -> service.register(request));
        verify(accounts).delete("subject");
    }

    @Test
    void failedEnableRollsBackAndDeletesRemoteUser() {
        doThrow(new IdentityProviderException()).when(accounts).enable("subject");
        assertThrows(IdentityProviderException.class, () -> service.register(request));
        verify(manager).rollback(status);
        verify(accounts).delete("subject");
    }

    @Test
    void failedCompensationReturnsSanitizedUnavailableError() {
        doThrow(new IllegalStateException("save failed")).when(users).saveAndFlush(any());
        doThrow(new IdentityProviderException()).when(accounts).delete("subject");
        assertThrows(IdentityProviderException.class, () -> service.register(request));
    }
}
