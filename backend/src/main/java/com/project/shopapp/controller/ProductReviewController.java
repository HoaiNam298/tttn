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
    private final com.project.shopapp.services.ReviewSubmissionService submissions;

    public ProductReviewController(
            ProductReviewService service,
            com.project.shopapp.services.ReviewSubmissionService submissions) {
        this.service = service;
        this.submissions = submissions;
    }

    @GetMapping
    ProductReviewOverviewResponse findAll(
            @PathVariable Long productId,
            @org.springframework.web.bind.annotation.RequestParam(required = false) Integer rating,
            @PageableDefault(size = 10, sort = "createdAt", direction = Sort.Direction.DESC)
                    Pageable pageable) {
        return service.findByProduct(productId, rating, pageable);
    }

    @PostMapping(consumes = org.springframework.http.MediaType.APPLICATION_JSON_VALUE)
    ResponseEntity<ProductReviewResponse> create(
            @PathVariable Long productId,
            Principal principal,
            @Valid @RequestBody CreateProductReviewRequest request) {
        ProductReviewResponse response = service.create(productId, principal.getName(), request);
        return ResponseEntity.created(
                        URI.create("/api/v1/products/" + productId + "/reviews/" + response.id()))
                .body(response);
    }

    @PostMapping(consumes = org.springframework.http.MediaType.MULTIPART_FORM_DATA_VALUE)
    ResponseEntity<ProductReviewResponse> createWithImages(
            @PathVariable Long productId,
            Principal principal,
            @Valid @org.springframework.web.bind.annotation.RequestPart("review")
                    CreateProductReviewRequest request,
            @org.springframework.web.bind.annotation.RequestPart(value = "images", required = false)
                    java.util.List<org.springframework.web.multipart.MultipartFile> images)
            throws java.io.IOException {
        ProductReviewResponse response =
                submissions.submit(
                        productId,
                        principal.getName(),
                        request,
                        images == null ? java.util.List.of() : images);
        return ResponseEntity.created(
                        URI.create("/api/v1/products/" + productId + "/reviews/" + response.id()))
                .body(response);
    }
}
