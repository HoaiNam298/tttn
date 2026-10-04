package com.project.shopapp.repositories;

import com.project.shopapp.model.Product;
import jakarta.persistence.LockModeType;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;

public interface ProductRepository extends JpaRepository<Product, Long> {
    @EntityGraph(attributePaths = {"category", "images"})
    Optional<Product> findDetailById(Long id);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    List<Product> findAllByIdInOrderById(Collection<Long> ids);

    Page<Product> findByNameContainingIgnoreCase(String keyword, Pageable pageable);

    Page<Product> findByCategoryId(Long categoryId, Pageable pageable);

    Page<Product> findByNameContainingIgnoreCaseAndCategoryId(
            String keyword, Long categoryId, Pageable pageable);
}
