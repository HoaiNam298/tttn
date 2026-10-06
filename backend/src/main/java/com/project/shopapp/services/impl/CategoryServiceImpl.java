package com.project.shopapp.services.impl;

import com.project.shopapp.dtos.CategoryRequest;
import com.project.shopapp.exceptions.DuplicateResourceException;
import com.project.shopapp.exceptions.ResourceNotFoundException;
import com.project.shopapp.model.Category;
import com.project.shopapp.repositories.CategoryRepository;
import com.project.shopapp.responses.CategoryResponse;
import com.project.shopapp.services.CategoryService;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
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
        String name = request.name().trim();
        repository
                .findByNameIgnoreCase(name)
                .ifPresent(
                        item -> {
                            throw new DuplicateResourceException(
                                    "Category already exists: " + name);
                        });
        return CategoryResponse.from(repository.save(new Category(name)));
    }

    @Transactional
    public CategoryResponse update(Long id, CategoryRequest request) {
        Category category = getEntity(id);
        String name = request.name().trim();
        repository
                .findByNameIgnoreCase(name)
                .filter(item -> !item.getId().equals(id))
                .ifPresent(
                        item -> {
                            throw new DuplicateResourceException(
                                    "Category already exists: " + name);
                        });
        category.rename(name);
        return CategoryResponse.from(category);
    }

    @Transactional
    public void delete(Long id) {
        repository.delete(getEntity(id));
    }

    public Category getEntity(Long id) {
        return repository
                .findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Category not found: " + id));
    }
}
