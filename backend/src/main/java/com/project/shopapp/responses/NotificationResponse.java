package com.project.shopapp.responses;

import com.project.shopapp.model.Notification;
import java.time.Instant;

public record NotificationResponse(
        Long id, String title, String body, String link, boolean read, Instant createdAt) {
    public static NotificationResponse from(Notification notification) {
        return new NotificationResponse(
                notification.getId(),
                notification.getTitle(),
                notification.getBody(),
                notification.getLink(),
                notification.isRead(),
                notification.getCreatedAt());
    }
}
