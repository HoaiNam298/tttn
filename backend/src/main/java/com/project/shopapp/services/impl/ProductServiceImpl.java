package com.project.shopapp.services.impl;

import com.project.shopapp.dtos.ProductInventoryRequest;
import com.project.shopapp.dtos.ProductRequest;
import com.project.shopapp.exceptions.ResourceNotFoundException;
import com.project.shopapp.model.OrderStatus;
import com.project.shopapp.model.Product;
import com.project.shopapp.model.ProductVariant;
import com.project.shopapp.repositories.OrderRepository;
import com.project.shopapp.repositories.ProductRepository;
import com.project.shopapp.responses.ProductDetailResponse;
import com.project.shopapp.responses.ProductResponse;
import com.project.shopapp.services.CategoryService;
import com.project.shopapp.services.ProductService;
import jakarta.persistence.criteria.Predicate;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class ProductServiceImpl implements ProductService {
    private final ProductRepository repository;
    private final CategoryService categoryService;
    private final OrderRepository orders;

    public ProductServiceImpl(
            ProductRepository repository, CategoryService categoryService, OrderRepository orders) {
        this.repository = repository;
        this.categoryService = categoryService;
        this.orders = orders;
    }

    public Page<ProductResponse> findAll(String keyword, Long categoryId, Pageable pageable) {
        return findAll(keyword, categoryId, null, null, pageable);
    }

    @Override
    public Page<ProductResponse> findAll(
            String keyword,
            Long categoryId,
            BigDecimal minPrice,
            BigDecimal maxPrice,
            Pageable pageable) {
        if ((minPrice != null && minPrice.signum() < 0)
                || (maxPrice != null && maxPrice.signum() < 0)
                || (minPrice != null && maxPrice != null && minPrice.compareTo(maxPrice) > 0)) {
            throw new IllegalArgumentException("Invalid price range");
        }
        Set<String> allowedSort = Set.of("id", "name", "price", "createdAt", "updatedAt");
        for (Sort.Order order : pageable.getSort()) {
            if (!allowedSort.contains(order.getProperty())) {
                throw new IllegalArgumentException("Unsupported product sort field");
            }
        }
        String value = keyword == null ? "" : keyword.trim();
        Specification<Product> filter =
                (root, query, builder) -> {
                    var predicates = new ArrayList<Predicate>();
                    if (!value.isEmpty()) {
                        String escaped =
                                value.toLowerCase(java.util.Locale.ROOT)
                                        .replace("\\", "\\\\")
                                        .replace("%", "\\%")
                                        .replace("_", "\\_");
                        predicates.add(
                                builder.like(
                                        builder.lower(root.get("name")),
                                        "%" + escaped + "%",
                                        '\\'));
                    }
                    if (categoryId != null) {
                        predicates.add(builder.equal(root.get("category").get("id"), categoryId));
                    }
                    if (minPrice != null) {
                        predicates.add(builder.greaterThanOrEqualTo(root.get("price"), minPrice));
                    }
                    if (maxPrice != null) {
                        predicates.add(builder.lessThanOrEqualTo(root.get("price"), maxPrice));
                    }
                    return builder.and(predicates.toArray(Predicate[]::new));
                };
        Sort sort =
                pageable.getSort().isSorted()
                        ? pageable.getSort()
                        : Sort.by(Sort.Direction.DESC, "createdAt");
        if (sort.getOrderFor("id") == null) {
            sort = sort.and(Sort.by(Sort.Direction.DESC, "id"));
        }
        Page<Product> products =
                repository.findAll(
                        filter,
                        PageRequest.of(
                                pageable.getPageNumber(),
                                Math.min(pageable.getPageSize(), 100),
                                sort));
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

    @Override
    @Transactional
    public ProductDetailResponse updateInventory(Long id, ProductInventoryRequest request) {
        Product product = getEntity(id);
        if (product.getVersion() != request.version()) {
            throw new IllegalStateException("Product inventory has changed. Reload before saving");
        }
        if (product.getVariants().isEmpty()
                && !request.variants().isEmpty()
                && orders.existsOpenLegacyItems(
                        id,
                        List.of(
                                OrderStatus.PENDING,
                                OrderStatus.CONFIRMED,
                                OrderStatus.SHIPPING))) {
            throw new IllegalStateException(
                    "Finish or cancel existing orders before converting this product to variants");
        }
        var ids = new HashSet<Long>();
        var names = new HashSet<String>();
        var skus = new HashSet<String>();
        for (var input : request.variants()) {
            if (!names.add(input.name().trim().toLowerCase(java.util.Locale.ROOT))
                    || !skus.add(input.sku().trim().toUpperCase(java.util.Locale.ROOT))) {
                throw new IllegalArgumentException("Duplicate variant name or SKU");
            }
            ProductVariant variant;
            if (input.id() == null) {
                variant = new ProductVariant(product);
                product.addVariant(variant);
            } else {
                if (!ids.add(input.id())) {
                    throw new IllegalArgumentException("Duplicate variant ID");
                }
                variant =
                        product.getVariants().stream()
                                .filter(item -> input.id().equals(item.getId()))
                                .findFirst()
                                .orElseThrow(
                                        () ->
                                                new IllegalArgumentException(
                                                        "Variant does not belong to this product"));
            }
            variant.update(
                    input.sku(),
                    input.name(),
                    input.color(),
                    input.size(),
                    input.capacity(),
                    input.price(),
                    input.stock(),
                    input.imageUrl(),
                    input.active());
        }
        if (product.getVariants().stream()
                .anyMatch(item -> item.getId() != null && !ids.contains(item.getId()))) {
            throw new IllegalArgumentException(
                    "Keep existing variants and deactivate them instead of deleting");
        }
        if (request.images().stream().distinct().count() != request.images().size()) {
            throw new IllegalArgumentException("Duplicate gallery image");
        }
        product.changeInventory(request.stock(), request.images());
        return ProductDetailResponse.from(repository.saveAndFlush(product));
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
                .findForUpdateById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found: " + id));
    }
}
