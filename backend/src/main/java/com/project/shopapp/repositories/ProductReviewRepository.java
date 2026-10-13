package com.project.shopapp.repositories;

import com.project.shopapp.model.ProductReview;
import java.util.Set;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ProductReviewRepository extends JpaRepository<ProductReview, Long> {
    @EntityGraph(attributePaths = {"user", "product"})
    Page<ProductReview> findByProductId(Long productId, Pageable pageable);

    @EntityGraph(attributePaths = {"user", "product"})
    Page<ProductReview> findByProductIdAndRating(Long productId, int rating, Pageable pageable);

    long countByProductId(Long productId);

    @Override
    @EntityGraph(attributePaths = {"user", "product"})
    Page<ProductReview> findAll(Pageable pageable);

    boolean existsByOrderItemId(Long orderItemId);

    @Query(
            value =
                    "SELECT r.order_item_id FROM product_reviews r JOIN order_items i ON i.id = r.order_item_id JOIN users u ON u.id = r.user_id WHERE i.order_id = :orderId AND u.phone_number = :phoneNumber",
            nativeQuery = true)
    Set<Long> reviewedItemIds(Long orderId, String phoneNumber);

    @Query(
            value =
                    "SELECT COALESCE(AVG(rating), 0) FROM product_reviews WHERE product_id = :productId",
            nativeQuery = true)
    double averageRating(@Param("productId") Long productId);
}
