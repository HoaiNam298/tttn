package com.project.shopapp.repositories;

import com.project.shopapp.model.OrderItem;
import com.project.shopapp.model.OrderStatus;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OrderItemRepository extends JpaRepository<OrderItem, Long> {
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
