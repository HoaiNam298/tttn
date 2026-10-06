package com.project.shopapp.repositories;

import com.project.shopapp.model.Category;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CategoryRepository extends JpaRepository<Category, Long> {
    Page<Category> findByNameContainingIgnoreCase(String keyword, Pageable pageable);

    Optional<Category> findByNameIgnoreCase(String name);
}
