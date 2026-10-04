package com.project.shopapp.repositories;

import com.project.shopapp.model.Order;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OrderRepository extends JpaRepository<Order, Long> {
    @EntityGraph(attributePaths = {"items", "items.product"})
    Optional<Order> findWithItemsByIdAndUserPhoneNumber(Long id, String phoneNumber);
}
