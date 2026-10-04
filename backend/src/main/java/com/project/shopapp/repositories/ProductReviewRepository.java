package com.project.shopapp.repositories;

import com.project.shopapp.model.ProductReview;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ProductReviewRepository extends JpaRepository<ProductReview, Long> {
    @EntityGraph(attributePaths = "user")
    Page<ProductReview> findByProductId(Long productId, Pageable pageable);

    boolean existsByOrderItemId(Long orderItemId);

    @Query("select coalesce(avg(r.rating), 0) from ProductReview r where r.product.id = :productId")
    double averageRating(@Param("productId") Long productId);
}
