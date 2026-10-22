package com.project.shopapp.services.impl;

import com.project.shopapp.components.MessageKeys;
import com.project.shopapp.components.TransactionLog;
import com.project.shopapp.dtos.request.CategoryRequest;
import com.project.shopapp.dtos.response.CategoryResponse;
import com.project.shopapp.exceptions.DuplicateResourceException;
import com.project.shopapp.exceptions.ResourceNotFoundException;
import com.project.shopapp.model.Category;
import com.project.shopapp.repositories.CategoryRepository;
import com.project.shopapp.services.CategoryService;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Slf4j
@Transactional(readOnly = true)
public class CategoryServiceImpl implements CategoryService {
    private final CategoryRepository repository;

    public CategoryServiceImpl(CategoryRepository repository) {
        this.repository = repository;
    }

    public List<CategoryResponse> findAll() {
        return repository.findAll().stream().map(CategoryResponse::from).toList();
    }

    @Override
    public Page<CategoryResponse> findPage(String keyword, Pageable pageable) {
        return repository
                .findByNameContainingIgnoreCase(keyword.trim(), pageable)
                .map(CategoryResponse::from);
    }

    public CategoryResponse findById(Long id) {
        return CategoryResponse.from(getEntity(id));
    }

    @Transactional
    public CategoryResponse create(CategoryRequest request) {
        String name = request.getName().trim();
        if (repository.findByNameIgnoreCase(name).isPresent()) {
            throw new DuplicateResourceException(MessageKeys.CATEGORY_ALREADY_EXISTS, name);
        }
        Category saved = repository.save(new Category(name));
        TransactionLog.info(log, "Category created categoryId={}", saved.getId());
        return CategoryResponse.from(saved);
    }

    @Transactional
    public CategoryResponse update(Long id, CategoryRequest request) {
        Category category = getEntity(id);
        String name = request.getName().trim();
        boolean duplicate =
                repository
                        .findByNameIgnoreCase(name)
                        .filter(item -> !item.getId().equals(id))
                        .isPresent();
        if (duplicate) {
            throw new DuplicateResourceException(MessageKeys.CATEGORY_ALREADY_EXISTS, name);
        }
        category.rename(name);
        TransactionLog.info(log, "Category updated categoryId={}", id);
        return CategoryResponse.from(category);
    }

    @Transactional
    public void delete(Long id) {
        repository.delete(getEntity(id));
        TransactionLog.info(log, "Category deleted categoryId={}", id);
    }

    public Category getEntity(Long id) {
        return repository
                .findById(id)
                .orElseThrow(
                        () -> new ResourceNotFoundException(MessageKeys.CATEGORY_NOT_FOUND, id));
    }
}
