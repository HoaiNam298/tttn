package com.project.shopapp.service;

import com.project.shopapp.dto.*;
import com.project.shopapp.exception.ResourceNotFoundException;
import com.project.shopapp.model.Product;
import com.project.shopapp.repository.ProductRepository;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class ProductService {
    private final ProductRepository repository;
    private final CategoryService categoryService;

    public ProductService(ProductRepository repository, CategoryService categoryService) {
        this.repository = repository;
        this.categoryService = categoryService;
    }

    public Page<ProductResponse> findAll(String keyword, Long categoryId, Pageable pageable) {
        String value = keyword == null ? "" : keyword.trim();
        Page<Product> products;
        if (categoryId != null && !value.isEmpty()) {
            products =
                    repository.findByNameContainingIgnoreCaseAndCategoryId(
                            value, categoryId, pageable);
        } else if (categoryId != null) {
            products = repository.findByCategoryId(categoryId, pageable);
        } else {
            products = repository.findByNameContainingIgnoreCase(value, pageable);
        }
        return products.map(ProductResponse::from);
    }

    public ProductResponse findById(Long id) {
        return ProductResponse.from(getEntity(id));
    }

    @Transactional
    public ProductResponse create(ProductRequest request) {
        Product product =
                new Product(
                        request.name().trim(),
                        request.price(),
                        request.thumbnail(),
                        request.description() == null ? "" : request.description().trim(),
                        categoryService.getEntity(request.categoryId()));
        return ProductResponse.from(repository.save(product));
    }

    public Product getEntity(Long id) {
        return repository
                .findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found: " + id));
    }
}
