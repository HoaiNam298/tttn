package com.project.shopapp.repositories;

import com.project.shopapp.enums.OrderStatus;
import com.project.shopapp.model.OrderItem;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface OrderItemRepository extends JpaRepository<OrderItem, Long> {
    List<OrderItem> findByOrderIdAndOrderUserPhoneNumberAndOrderStatus(
            Long orderId, String phoneNumber, OrderStatus status);

    @Query(value = "SELECT * FROM order_items WHERE id = :id FOR UPDATE", nativeQuery = true)
    Optional<OrderItem> lockById(Long id);

    @EntityGraph(attributePaths = {"product", "order", "order.user"})
    Optional<OrderItem>
            findFirstByOrder_IdAndProduct_IdAndOrder_User_PhoneNumberAndOrder_StatusOrderByIdAsc(
                    Long orderId, Long productId, String phoneNumber, OrderStatus status);

    @EntityGraph(attributePaths = {"product", "order", "order.user"})
    Optional<OrderItem>
            findByOrder_IdAndProduct_IdAndVariant_IdAndOrder_User_PhoneNumberAndOrder_Status(
                    Long orderId,
                    Long productId,
                    Long variantId,
                    String phoneNumber,
                    OrderStatus status);
}
