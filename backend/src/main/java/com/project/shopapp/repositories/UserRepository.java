package com.project.shopapp.repositories;

import com.project.shopapp.model.User;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByPhoneNumber(String phoneNumber);

    @Query(
            value = "SELECT * FROM users WHERE phone_number = :phoneNumber FOR UPDATE",
            nativeQuery = true)
    Optional<User> findForUpdateByPhoneNumber(String phoneNumber);

    boolean existsByPhoneNumber(String phoneNumber);
}
