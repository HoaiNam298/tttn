package com.project.shopapp.repositories;

import com.project.shopapp.model.User;
import jakarta.persistence.LockModeType;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;

public interface UserRepository extends JpaRepository<User, Long> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select u from User u where u.id = :id")
    Optional<User> findForUpdateById(Long id);

    Optional<User> findByPhoneNumber(String phoneNumber);

    Optional<User> findByKeycloakUserId(String keycloakUserId);

    boolean existsByPhoneNumber(String phoneNumber);

    @Query(
            value =
                    """
                    SELECT *
                    FROM users
                    WHERE phone_number = :phoneNumber
                    FOR UPDATE
                    """,
            nativeQuery = true)
    Optional<User> findForUpdateByPhoneNumber(String phoneNumber);
}
