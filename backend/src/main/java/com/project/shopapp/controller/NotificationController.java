package com.project.shopapp.controller;

import com.project.shopapp.responses.NotificationResponse;
import com.project.shopapp.services.NotificationService;
import java.security.Principal;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/notifications")
public class NotificationController {
    private final NotificationService service;

    public NotificationController(NotificationService service) {
        this.service = service;
    }

    @GetMapping
    Page<NotificationResponse> findAll(
            Principal principal, @PageableDefault(size = 10) Pageable pageable) {
        return service.findOwned(principal.getName(), pageable);
    }

    @GetMapping("/unread-count")
    long unreadCount(Principal principal) {
        return service.unreadCount(principal.getName());
    }

    @PutMapping("/{id}/read")
    ResponseEntity<Void> markRead(Principal principal, @PathVariable Long id) {
        service.markRead(principal.getName(), id);
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/read-all")
    ResponseEntity<Void> markAllRead(Principal principal) {
        service.markAllRead(principal.getName());
        return ResponseEntity.noContent().build();
    }
}
