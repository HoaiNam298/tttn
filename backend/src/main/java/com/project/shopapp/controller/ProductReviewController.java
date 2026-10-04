package com.project.shopapp.controller;

import com.project.shopapp.dtos.CreateProductReviewRequest;
import com.project.shopapp.responses.ProductReviewOverviewResponse;
import com.project.shopapp.responses.ProductReviewResponse;
import com.project.shopapp.services.ProductReviewService;
import jakarta.validation.Valid;
import java.net.URI;
import java.security.Principal;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/products/{productId}/reviews")
public class ProductReviewController {
    private final ProductReviewService service;

    public ProductReviewController(ProductReviewService service) {
        this.service = service;
    }

    @GetMapping
    ProductReviewOverviewResponse findAll(
            @PathVariable Long productId,
            @PageableDefault(size = 10, sort = "createdAt", direction = Sort.Direction.DESC)
                    Pageable pageable) {
        return service.findByProduct(productId, pageable);
    }

    @PostMapping
    ResponseEntity<ProductReviewResponse> create(
            @PathVariable Long productId,
            Principal principal,
            @Valid @RequestBody CreateProductReviewRequest request) {
        ProductReviewResponse response = service.create(productId, principal.getName(), request);
        return ResponseEntity.created(
                        URI.create("/api/v1/products/" + productId + "/reviews/" + response.id()))
                .body(response);
    }
}
