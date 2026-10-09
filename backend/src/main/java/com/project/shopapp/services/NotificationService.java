package com.project.shopapp.services;

import com.project.shopapp.responses.NotificationResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface NotificationService {
    Page<NotificationResponse> findOwned(String phoneNumber, Pageable pageable);

    long unreadCount(String phoneNumber);

    void markRead(String phoneNumber, Long id);

    void markAllRead(String phoneNumber);
}
