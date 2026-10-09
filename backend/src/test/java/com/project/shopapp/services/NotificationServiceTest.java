package com.project.shopapp.services;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.project.shopapp.exceptions.ResourceNotFoundException;
import com.project.shopapp.repositories.NotificationRepository;
import com.project.shopapp.services.impl.NotificationServiceImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class NotificationServiceTest {
    @Mock NotificationRepository repository;

    @Test
    void cannotMarkAnotherUsersNotification() {
        var service = new NotificationServiceImpl(repository);
        assertThrows(ResourceNotFoundException.class, () -> service.markRead("buyer", 8L));
        verify(repository, never()).markRead(any(), any(), any());
    }

    @Test
    void readIsIdempotentAndOwnerScoped() {
        when(repository.existsByIdAndUserPhoneNumber(8L, "buyer")).thenReturn(true);
        new NotificationServiceImpl(repository).markRead("buyer", 8L);
        verify(repository).markRead(eq(8L), eq("buyer"), any());
    }

    @Test
    void readAllIsOwnerScoped() {
        new NotificationServiceImpl(repository).markAllRead("buyer");
        verify(repository).markAllRead(eq("buyer"), any());
    }
}
