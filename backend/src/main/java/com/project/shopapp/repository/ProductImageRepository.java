package com.project.shopapp.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import com.project.shopapp.model.ProductImage;

public interface ProductImageRepository extends JpaRepository<ProductImage, Long> {}
