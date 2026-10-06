package com.project.shopapp.repositories;

import com.project.shopapp.model.User;
import jakarta.persistence.LockModeType;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;

public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByPhoneNumber(String phoneNumber);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select u from User u where u.phoneNumber = :phoneNumber")
    Optional<User> findForUpdateByPhoneNumber(String phoneNumber);

    boolean existsByPhoneNumber(String phoneNumber);
}
