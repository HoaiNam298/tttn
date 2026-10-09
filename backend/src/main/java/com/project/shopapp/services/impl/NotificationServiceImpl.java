package com.project.shopapp.services.impl;

import com.project.shopapp.exceptions.ResourceNotFoundException;
import com.project.shopapp.repositories.NotificationRepository;
import com.project.shopapp.responses.NotificationResponse;
import com.project.shopapp.services.NotificationService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
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
        return repository
                .findByUserPhoneNumber(phoneNumber, bounded)
                .map(NotificationResponse::from);
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
        repository.markRead(id, phoneNumber, java.time.Instant.now());
    }

    @Override
    @Transactional
    public void markAllRead(String phoneNumber) {
        repository.markAllRead(phoneNumber, java.time.Instant.now());
    }
}
