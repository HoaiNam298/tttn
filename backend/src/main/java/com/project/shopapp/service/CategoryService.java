package com.project.shopapp.service;

import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.project.shopapp.dto.*;
import com.project.shopapp.exception.*;
import com.project.shopapp.model.Category;
import com.project.shopapp.repository.CategoryRepository;

@Service
@Transactional(readOnly = true)
public class CategoryService {
    private final CategoryRepository repository;
    public CategoryService(CategoryRepository repository) { this.repository = repository; }

    public List<CategoryResponse> findAll() {
        return repository.findAll().stream().map(CategoryResponse::from).toList();
    }

    public CategoryResponse findById(Long id) { return CategoryResponse.from(getEntity(id)); }

    @Transactional
    public CategoryResponse create(CategoryRequest request) {
        String name = request.name().trim();
        repository.findByNameIgnoreCase(name).ifPresent(item -> {
            throw new DuplicateResourceException("Category already exists: " + name);
        });
        return CategoryResponse.from(repository.save(new Category(name)));
    }

    @Transactional
    public CategoryResponse update(Long id, CategoryRequest request) {
        Category category = getEntity(id);
        String name = request.name().trim();
        repository.findByNameIgnoreCase(name)
                .filter(item -> !item.getId().equals(id))
                .ifPresent(item -> { throw new DuplicateResourceException("Category already exists: " + name); });
        category.rename(name);
        return CategoryResponse.from(category);
    }

    @Transactional
    public void delete(Long id) { repository.delete(getEntity(id)); }

    public Category getEntity(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Category not found: " + id));
    }
}
