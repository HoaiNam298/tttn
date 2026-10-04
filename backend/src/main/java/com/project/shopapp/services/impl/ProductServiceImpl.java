package com.project.shopapp.services.impl;

import com.project.shopapp.dtos.ProductRequest;
import com.project.shopapp.exceptions.ResourceNotFoundException;
import com.project.shopapp.model.Product;
import com.project.shopapp.repositories.ProductRepository;
import com.project.shopapp.responses.ProductDetailResponse;
import com.project.shopapp.responses.ProductResponse;
import com.project.shopapp.services.CategoryService;
import com.project.shopapp.services.ProductService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class ProductServiceImpl implements ProductService {
    private final ProductRepository repository;
    private final CategoryService categoryService;

    public ProductServiceImpl(ProductRepository repository, CategoryService categoryService) {
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

    public ProductDetailResponse findById(Long id) {
        Product product =
                repository
                        .findDetailById(id)
                        .orElseThrow(
                                () -> new ResourceNotFoundException("Product not found: " + id));
        return ProductDetailResponse.from(product);
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

    @Transactional
    public ProductResponse update(Long id, ProductRequest request) {
        Product product = getEntity(id);
        product.update(
                request.name().trim(),
                request.price(),
                request.thumbnail(),
                request.description() == null ? "" : request.description().trim(),
                categoryService.getEntity(request.categoryId()));
        return ProductResponse.from(repository.save(product));
    }

    @Transactional
    public void delete(Long id) {
        repository.delete(getEntity(id));
    }

    public Product getEntity(Long id) {
        return repository
                .findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found: " + id));
    }
}
