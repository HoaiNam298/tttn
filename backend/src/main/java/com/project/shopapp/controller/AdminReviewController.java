package com.project.shopapp.controller;

import com.project.shopapp.dtos.ReviewReplyRequest;
import com.project.shopapp.responses.ProductReviewResponse;
import com.project.shopapp.services.ProductReviewService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/admin/reviews")
public class AdminReviewController {
    private final ProductReviewService service;

    public AdminReviewController(ProductReviewService service) {
        this.service = service;
    }

    @GetMapping
    Page<ProductReviewResponse> findAll(@PageableDefault(size = 10) Pageable pageable) {
        return service.findAll(pageable);
    }

    @PutMapping("/{id}/reply")
    ProductReviewResponse reply(
            @PathVariable Long id, @Valid @RequestBody ReviewReplyRequest request) {
        return service.reply(id, request);
    }
}
