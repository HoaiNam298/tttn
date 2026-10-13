package com.project.shopapp.services.impl;

import com.project.shopapp.components.TransactionLog;
import com.project.shopapp.dtos.response.NotificationResponse;
import com.project.shopapp.exceptions.ResourceNotFoundException;
import com.project.shopapp.model.Notification;
import com.project.shopapp.repositories.NotificationRepository;
import com.project.shopapp.services.NotificationService;
import java.time.Instant;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Slf4j
@Transactional(readOnly = true)
public class NotificationServiceImpl implements NotificationService {
    private final NotificationRepository repository;

    public NotificationServiceImpl(NotificationRepository repository) {
        this.repository = repository;
    }

    @Override
    public Page<NotificationResponse> findOwned(String phoneNumber, Pageable pageable) {
        Pageable bounded =
                PageRequest.of(
                        pageable.getPageNumber(),
                        Math.min(50, pageable.getPageSize()),
                        Sort.by(Sort.Direction.DESC, "id"));
        Page<Notification> notifications = repository.findByUserPhoneNumber(phoneNumber, bounded);
        return notifications.map(NotificationResponse::from);
    }

    @Override
    public long unreadCount(String phoneNumber) {
        return repository.countByUserPhoneNumberAndReadFalse(phoneNumber);
    }

    @Override
    @Transactional
    public void markRead(String phoneNumber, Long id) {
        if (!repository.existsByIdAndUserPhoneNumber(id, phoneNumber)) {
            throw new ResourceNotFoundException("Notification not found");
        }
        int updated = repository.markRead(id, phoneNumber, Instant.now());
        TransactionLog.debug(
                log,
                "Notification read operation completed notificationId={} updated={}",
                id,
                updated);
    }

    @Override
    @Transactional
    public void markAllRead(String phoneNumber) {
        int updated = repository.markAllRead(phoneNumber, Instant.now());
        TransactionLog.debug(log, "Notification bulk read operation completed updated={}", updated);
    }
}
