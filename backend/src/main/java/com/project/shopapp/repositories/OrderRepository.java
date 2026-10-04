package com.project.shopapp.repositories;

import com.project.shopapp.model.Order;
import com.project.shopapp.model.OrderStatus;
import jakarta.persistence.LockModeType;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;

public interface OrderRepository extends JpaRepository<Order, Long> {
    @EntityGraph(attributePaths = {"items", "items.product"})
    Optional<Order> findWithItemsByIdAndUserPhoneNumber(Long id, String phoneNumber);

    Page<Order> findAllByUserPhoneNumber(String phoneNumber, Pageable pageable);

    Page<Order> findAllByStatus(OrderStatus status, Pageable pageable);

    @EntityGraph(attributePaths = {"items", "items.product"})
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<Order> findWithItemsById(Long id);
}
