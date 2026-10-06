package com.project.shopapp.repositories;

import com.project.shopapp.model.Order;
import com.project.shopapp.model.OrderStatus;
import jakarta.persistence.LockModeType;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;

public interface OrderRepository extends JpaRepository<Order, Long> {
    long countByVoucher_IdAndUser_IdAndStatusNot(Long voucherId, Long userId, OrderStatus status);

    @EntityGraph(attributePaths = {"items", "items.product"})
    Optional<Order> findByUserIdAndRequestId(Long userId, UUID requestId);

    @org.springframework.data.jpa.repository.Query(
            "select (count(i) > 0) from OrderItem i where i.product.id = :productId and i.variant is null and i.order.status in :statuses")
    boolean existsOpenLegacyItems(Long productId, java.util.Collection<OrderStatus> statuses);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<Order> findByIdAndUserPhoneNumber(Long id, String phoneNumber);

    @EntityGraph(attributePaths = {"items", "items.product"})
    Optional<Order> findWithItemsByIdAndUserPhoneNumber(Long id, String phoneNumber);

    Page<Order> findAllByUserPhoneNumber(String phoneNumber, Pageable pageable);

    Page<Order> findAllByUserPhoneNumberAndStatus(
            String phoneNumber, com.project.shopapp.model.OrderStatus status, Pageable pageable);

    Page<Order> findAllByStatus(OrderStatus status, Pageable pageable);

    @EntityGraph(attributePaths = {"items", "items.product"})
    Optional<Order> findDetailById(Long id);

    @EntityGraph(attributePaths = {"items"})
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<Order> findWithItemsById(Long id);
}
