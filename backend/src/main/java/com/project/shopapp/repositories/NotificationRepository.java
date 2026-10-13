package com.project.shopapp.repositories;

import com.project.shopapp.model.Notification;
import java.time.Instant;
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
            value =
                    """
                    UPDATE notifications n
                    SET read = TRUE,
                        updated_at = :now
                    WHERE n.id = :id
                      AND EXISTS (
                          SELECT 1
                          FROM users u
                          WHERE u.id = n.user_id
                            AND u.phone_number = :phoneNumber
                      )
                      AND read = FALSE
                    """,
            nativeQuery = true)
    int markRead(Long id, String phoneNumber, Instant now);

    @Modifying
    @Query(
            value =
                    """
                    UPDATE notifications n
                    SET read = TRUE,
                        updated_at = :now
                    WHERE EXISTS (
                        SELECT 1
                        FROM users u
                        WHERE u.id = n.user_id
                          AND u.phone_number = :phoneNumber
                    )
                      AND read = FALSE
                    """,
            nativeQuery = true)
    int markAllRead(String phoneNumber, Instant now);
}
