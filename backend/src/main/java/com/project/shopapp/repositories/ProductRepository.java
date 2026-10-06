package com.project.shopapp.repositories;

import com.project.shopapp.model.Product;
import jakarta.persistence.LockModeType;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Lock;

public interface ProductRepository
        extends JpaRepository<Product, Long>, JpaSpecificationExecutor<Product> {
    @Override
    @EntityGraph(attributePaths = {"category"})
    Page<Product> findAll(Specification<Product> specification, Pageable pageable);

    @EntityGraph(attributePaths = {"category", "images"})
    Optional<Product> findDetailById(Long id);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<Product> findForUpdateById(Long id);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    List<Product> findAllByIdInOrderById(Collection<Long> ids);

    @EntityGraph(attributePaths = {"category"})
    Page<Product> findByNameContainingIgnoreCase(String keyword, Pageable pageable);

    @EntityGraph(attributePaths = {"category"})
    Page<Product> findByCategoryId(Long categoryId, Pageable pageable);

    @EntityGraph(attributePaths = {"category"})
    Page<Product> findByNameContainingIgnoreCaseAndCategoryId(
            String keyword, Long categoryId, Pageable pageable);
}
