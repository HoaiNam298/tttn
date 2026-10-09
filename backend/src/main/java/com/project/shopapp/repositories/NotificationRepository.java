package com.project.shopapp.repositories;

import com.project.shopapp.model.Notification;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

public interface NotificationRepository extends JpaRepository<Notification, Long> {
    Page<Notification> findByUserPhoneNumber(String phoneNumber, Pageable pageable);

    long countByUserPhoneNumberAndReadFalse(String phoneNumber);

    boolean existsByIdAndUserPhoneNumber(Long id, String phoneNumber);

    @Modifying
    @Query(
            "update Notification n set n.read = true, n.updatedAt = :now where n.id = :id and n.user.phoneNumber = :phoneNumber and n.read = false")
    int markRead(Long id, String phoneNumber, java.time.Instant now);

    @Modifying
    @Query(
            "update Notification n set n.read = true, n.updatedAt = :now where n.user.phoneNumber = :phoneNumber and n.read = false")
    int markAllRead(String phoneNumber, java.time.Instant now);
}
