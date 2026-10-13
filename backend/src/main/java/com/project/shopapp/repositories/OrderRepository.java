package com.project.shopapp.repositories;

import com.project.shopapp.enums.OrderStatus;
import com.project.shopapp.model.Order;
import jakarta.persistence.LockModeType;
import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;

public interface OrderRepository extends JpaRepository<Order, Long> {
    @Query(
            value =
                    "SELECT o.id FROM orders o WHERE o.payment_method = 'VNPAY' AND o.payment_status = 'PENDING' AND o.status = 'PENDING' AND o.payment_expires_at <= :now ORDER BY o.id",
            nativeQuery = true)
    List<Long> findExpiredPaymentIds(Instant now, Pageable pageable);

    long countByVoucher_IdAndUser_IdAndStatusNot(Long voucherId, Long userId, OrderStatus status);

    @EntityGraph(attributePaths = {"items", "items.product"})
    Optional<Order> findByUserIdAndRequestId(Long userId, UUID requestId);

    @Query(
            value =
                    "SELECT EXISTS (SELECT 1 FROM order_items i JOIN orders o ON o.id = i.order_id WHERE i.product_id = :productId AND i.variant_id IS NULL AND o.status IN (:statuses))",
            nativeQuery = true)
    boolean existsOpenLegacyItems(Long productId, Collection<OrderStatus> statuses);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<Order> findByIdAndUserPhoneNumber(Long id, String phoneNumber);

    @EntityGraph(attributePaths = {"items", "items.product"})
    Optional<Order> findWithItemsByIdAndUserPhoneNumber(Long id, String phoneNumber);

    Page<Order> findAllByUserPhoneNumber(String phoneNumber, Pageable pageable);

    Page<Order> findAllByUserPhoneNumberAndStatus(
            String phoneNumber, OrderStatus status, Pageable pageable);

    Page<Order> findAllByStatus(OrderStatus status, Pageable pageable);

    @EntityGraph(attributePaths = {"items", "items.product"})
    Optional<Order> findDetailById(Long id);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<Order> findWithItemsById(Long id);
}
