package com.project.shopapp.services.impl;

import com.project.shopapp.components.I18nMessages;
import com.project.shopapp.components.MessageKeys;
import com.project.shopapp.components.TransactionLog;
import com.project.shopapp.dtos.response.NotificationResponse;
import com.project.shopapp.exceptions.ResourceNotFoundException;
import com.project.shopapp.model.Notification;
import com.project.shopapp.repositories.NotificationRepository;
import com.project.shopapp.services.NotificationService;
import java.time.Instant;
import java.util.regex.Pattern;
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
    private final I18nMessages messages;
    private static final Pattern ORDER_EVENT =
            Pattern.compile(
                    "^order:([1-9][0-9]*):(PENDING|CONFIRMED|SHIPPING|DELIVERED|COMPLETED|CANCELLED)$");
    private static final Pattern REVIEW_REPLY_EVENT = Pattern.compile("^review-reply:[1-9][0-9]*$");

    public NotificationServiceImpl(NotificationRepository repository, I18nMessages messages) {
        this.repository = repository;
        this.messages = messages;
    }

    @Override
    public Page<NotificationResponse> findOwned(String phoneNumber, Pageable pageable) {
        Pageable bounded =
                PageRequest.of(
                        pageable.getPageNumber(),
                        Math.min(50, pageable.getPageSize()),
                        Sort.by(Sort.Direction.DESC, "id"));
        Page<Notification> notifications = repository.findByUserPhoneNumber(phoneNumber, bounded);
        return notifications.map(this::toResponse);
    }

    private NotificationResponse toResponse(Notification notification) {
        NotificationResponse response = NotificationResponse.from(notification);
        String eventKey = notification.getEventKey();
        if (eventKey == null) {
            return response;
        }
        var orderEvent = ORDER_EVENT.matcher(eventKey);
        if (orderEvent.matches()) {
            response.setTitle(
                    messages.get(MessageKeys.NOTIFICATION_ORDER_TITLE, orderEvent.group(1)));
            response.setBody(
                    messages.get(
                            switch (orderEvent.group(2)) {
                                case "PENDING" -> MessageKeys.NOTIFICATION_ORDER_PENDING;
                                case "CONFIRMED" -> MessageKeys.NOTIFICATION_ORDER_CONFIRMED;
                                case "SHIPPING" -> MessageKeys.NOTIFICATION_ORDER_SHIPPING;
                                case "DELIVERED" -> MessageKeys.NOTIFICATION_ORDER_DELIVERED;
                                case "COMPLETED" -> MessageKeys.NOTIFICATION_ORDER_COMPLETED;
                                case "CANCELLED" -> MessageKeys.NOTIFICATION_ORDER_CANCELLED;
                                default ->
                                        throw new IllegalStateException(
                                                "Unsupported notification event");
                            }));
        } else if (REVIEW_REPLY_EVENT.matcher(eventKey).matches()) {
            response.setTitle(messages.get(MessageKeys.NOTIFICATION_REVIEW_REPLY_TITLE));
            response.setBody(messages.get(MessageKeys.NOTIFICATION_REVIEW_REPLY_BODY));
        }
        return response;
    }

    @Override
    public long unreadCount(String phoneNumber) {
        return repository.countByUserPhoneNumberAndReadFalse(phoneNumber);
    }

    @Override
    @Transactional
    public void markRead(String phoneNumber, Long id) {
        if (!repository.existsByIdAndUserPhoneNumber(id, phoneNumber)) {
            throw new ResourceNotFoundException(MessageKeys.NOTIFICATION_NOT_FOUND);
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
