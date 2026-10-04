package com.project.shopapp.services;

import com.project.shopapp.dtos.CategoryRequest;
import com.project.shopapp.model.Category;
import com.project.shopapp.responses.CategoryResponse;
import java.util.List;

public interface CategoryService {
    List<CategoryResponse> findAll();

    CategoryResponse findById(Long id);

    CategoryResponse create(CategoryRequest request);

    CategoryResponse update(Long id, CategoryRequest request);

    void delete(Long id);

    Category getEntity(Long id);
}
