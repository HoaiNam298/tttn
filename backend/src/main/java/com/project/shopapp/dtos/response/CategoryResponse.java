package com.project.shopapp.dtos.response;

import com.project.shopapp.model.Category;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CategoryResponse {

    private Long id;

    private String name;

    public static CategoryResponse from(Category category) {
        return CategoryResponse.builder().id(category.getId()).name(category.getName()).build();
    }
}
