package com.project.shopapp.repositories;

import com.project.shopapp.model.Favorite;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FavoriteRepository extends JpaRepository<Favorite, Long> {
    @EntityGraph(attributePaths = {"product", "product.category"})
    Page<Favorite> findByUserPhoneNumber(String phoneNumber, Pageable pageable);

    boolean existsByUserIdAndProductId(Long userId, Long productId);

    boolean existsByUserPhoneNumberAndProductId(String phoneNumber, Long productId);

    void deleteByUserIdAndProductId(Long userId, Long productId);
}
