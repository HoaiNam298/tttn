package com.project.shopapp.services.impl;

import com.project.shopapp.components.MessageKeys;
import com.project.shopapp.components.TransactionLog;
import com.project.shopapp.dtos.request.ProductInventoryRequest;
import com.project.shopapp.dtos.request.ProductRequest;
import com.project.shopapp.dtos.response.ProductDetailResponse;
import com.project.shopapp.dtos.response.ProductResponse;
import com.project.shopapp.enums.OrderStatus;
import com.project.shopapp.exceptions.LocalizedArgumentException;
import com.project.shopapp.exceptions.LocalizedStateException;
import com.project.shopapp.exceptions.ResourceNotFoundException;
import com.project.shopapp.model.Product;
import com.project.shopapp.model.ProductVariant;
import com.project.shopapp.repositories.OrderRepository;
import com.project.shopapp.repositories.ProductRepository;
import com.project.shopapp.services.CategoryService;
import com.project.shopapp.services.ProductService;
import jakarta.persistence.criteria.Predicate;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Slf4j
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
            throw new LocalizedArgumentException(MessageKeys.PRODUCT_INVALID_PRICE_RANGE);
        }
        Set<String> allowedSort = Set.of("id", "name", "price", "createdAt", "updatedAt");
        for (Sort.Order order : pageable.getSort()) {
            if (!allowedSort.contains(order.getProperty())) {
                throw new LocalizedArgumentException(MessageKeys.PRODUCT_INVALID_SORT);
            }
        }
        String value = keyword == null ? "" : keyword.trim();
        Specification<Product> filter =
                (root, query, builder) -> {
                    var predicates = new ArrayList<Predicate>();
                    if (!value.isEmpty()) {
                        String escaped =
                                value.toLowerCase(Locale.ROOT)
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
                                () ->
                                        new ResourceNotFoundException(
                                                MessageKeys.PRODUCT_NOT_FOUND, id));
        return ProductDetailResponse.from(product);
    }

    @Override
    @Transactional
    public ProductDetailResponse updateInventory(Long id, ProductInventoryRequest request) {
        Product product = getEntity(id);
        if (product.getVersion() != request.getVersion()) {
            throw new LocalizedStateException(MessageKeys.PRODUCT_INVENTORY_CHANGED);
        }
        if (product.getVariants().isEmpty()
                && !request.getVariants().isEmpty()
                && orders.existsOpenLegacyItems(
                        id,
                        List.of(
                                OrderStatus.PENDING,
                                OrderStatus.CONFIRMED,
                                OrderStatus.SHIPPING))) {
            throw new LocalizedStateException(MessageKeys.PRODUCT_CONVERSION_UNAVAILABLE);
        }
        var ids = new HashSet<Long>();
        var names = new HashSet<String>();
        var skus = new HashSet<String>();
        for (var input : request.getVariants()) {
            if (!names.add(input.getName().trim().toLowerCase(Locale.ROOT))
                    || !skus.add(input.getSku().trim().toUpperCase(Locale.ROOT))) {
                throw new LocalizedArgumentException(MessageKeys.PRODUCT_DUPLICATE_VARIANT);
            }
            ProductVariant variant;
            if (input.getId() == null) {
                variant = new ProductVariant(product);
                product.addVariant(variant);
            } else {
                if (!ids.add(input.getId())) {
                    throw new LocalizedArgumentException(MessageKeys.PRODUCT_DUPLICATE_VARIANT_ID);
                }
                variant = findVariant(product, input.getId());
            }
            variant.update(
                    input.getSku(),
                    input.getName(),
                    input.getColor(),
                    input.getSize(),
                    input.getCapacity(),
                    input.getPrice(),
                    input.getStock(),
                    input.getImageUrl(),
                    input.isActive());
        }
        if (product.getVariants().stream()
                .anyMatch(item -> item.getId() != null && !ids.contains(item.getId()))) {
            throw new LocalizedArgumentException(MessageKeys.PRODUCT_VARIANT_REMOVAL_UNAVAILABLE);
        }
        if (request.getImages().stream().distinct().count() != request.getImages().size()) {
            throw new LocalizedArgumentException(MessageKeys.PRODUCT_DUPLICATE_IMAGE);
        }
        product.changeInventory(request.getStock(), request.getImages());
        Product saved = repository.saveAndFlush(product);
        TransactionLog.info(
                log,
                "Product inventory updated productId={} stock={} variants={}",
                saved.getId(),
                saved.getStock(),
                saved.getVariants().size());
        return ProductDetailResponse.from(saved);
    }

    @Transactional
    public ProductResponse create(ProductRequest request) {
        Product product =
                Product.builder()
                        .name(request.getName().trim())
                        .price(request.getPrice())
                        .thumbnail(request.getThumbnail())
                        .description(
                                request.getDescription() == null
                                        ? ""
                                        : request.getDescription().trim())
                        .category(categoryService.getEntity(request.getCategoryId()))
                        .build();
        Product saved = repository.save(product);
        TransactionLog.info(log, "Product created productId={}", saved.getId());
        return ProductResponse.from(saved);
    }

    @Transactional
    public ProductResponse update(Long id, ProductRequest request) {
        Product product = getEntity(id);
        product.update(
                request.getName().trim(),
                request.getPrice(),
                request.getThumbnail(),
                request.getDescription() == null ? "" : request.getDescription().trim(),
                categoryService.getEntity(request.getCategoryId()));
        Product saved = repository.save(product);
        TransactionLog.info(log, "Product updated productId={}", saved.getId());
        return ProductResponse.from(saved);
    }

    @Transactional
    public void delete(Long id) {
        repository.delete(getEntity(id));
        TransactionLog.info(log, "Product deleted productId={}", id);
    }

    public Product getEntity(Long id) {
        return repository
                .findForUpdateById(id)
                .orElseThrow(
                        () -> new ResourceNotFoundException(MessageKeys.PRODUCT_NOT_FOUND, id));
    }

    private ProductVariant findVariant(Product product, Long variantId) {
        ProductVariant variant =
                product.getVariants().stream()
                        .filter(item -> variantId.equals(item.getId()))
                        .findFirst()
                        .orElse(null);
        if (variant == null) {
            throw new LocalizedArgumentException(MessageKeys.PRODUCT_VARIANT_WRONG_PRODUCT);
        }
        return variant;
    }
}
