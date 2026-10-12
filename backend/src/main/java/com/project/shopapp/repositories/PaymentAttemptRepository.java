package com.project.shopapp.repositories;

import com.project.shopapp.model.PaymentAttempt;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PaymentAttemptRepository extends JpaRepository<PaymentAttempt, Long> {
    Optional<PaymentAttempt> findByOrder_Id(Long orderId);

    Optional<PaymentAttempt> findByTxnRef(String txnRef);
}
