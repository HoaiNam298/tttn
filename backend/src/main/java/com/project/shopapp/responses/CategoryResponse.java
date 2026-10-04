package com.project.shopapp.responses;

import com.project.shopapp.model.Category;

public record CategoryResponse(Long id, String name) {
    public static CategoryResponse from(Category category) {
        return new CategoryResponse(category.getId(), category.getName());
    }
}
