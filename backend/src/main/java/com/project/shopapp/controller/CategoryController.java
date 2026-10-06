package com.project.shopapp.controller;

import com.project.shopapp.dtos.CategoryRequest;
import com.project.shopapp.responses.CategoryResponse;
import com.project.shopapp.services.CategoryService;
import jakarta.validation.Valid;
import java.net.URI;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/categories")
public class CategoryController {
    private final CategoryService service;

    public CategoryController(CategoryService service) {
        this.service = service;
    }

    @GetMapping
    public List<CategoryResponse> findAll() {
        return service.findAll();
    }

    @GetMapping("/{id}")
    public CategoryResponse findById(@PathVariable Long id) {
        return service.findById(id);
    }

    @GetMapping("/page")
    public Page<CategoryResponse> findPage(
            @RequestParam(defaultValue = "") String keyword,
            @PageableDefault(size = 10, sort = "id") Pageable pageable) {
        return service.findPage(keyword, pageable);
    }

    @PostMapping
    public ResponseEntity<CategoryResponse> create(@Valid @RequestBody CategoryRequest request) {
        CategoryResponse created = service.create(request);
        return ResponseEntity.created(URI.create("/api/v1/categories/" + created.id()))
                .body(created);
    }

    @PutMapping("/{id}")
    public CategoryResponse update(
            @PathVariable Long id, @Valid @RequestBody CategoryRequest request) {
        return service.update(id, request);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        service.delete(id);
        return ResponseEntity.noContent().build();
    }
}
