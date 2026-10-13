package com.project.shopapp.controller;

import com.project.shopapp.dtos.request.CreateProductReviewRequest;
import com.project.shopapp.dtos.response.ProductReviewOverviewResponse;
import com.project.shopapp.dtos.response.ProductReviewResponse;
import com.project.shopapp.services.ProductReviewService;
import com.project.shopapp.services.ReviewSubmissionService;
import jakarta.validation.Valid;
import java.io.IOException;
import java.security.Principal;
import java.util.List;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/v1/products/{productId}/reviews")
public class ProductReviewController {
    private final ProductReviewService service;
    private final ReviewSubmissionService submissions;

    public ProductReviewController(
            ProductReviewService service, ReviewSubmissionService submissions) {
        this.service = service;
        this.submissions = submissions;
    }

    @GetMapping
    ProductReviewOverviewResponse findAll(
            @PathVariable Long productId,
            @RequestParam(required = false) Integer rating,
            @PageableDefault(size = 10, sort = "createdAt", direction = Sort.Direction.DESC)
                    Pageable pageable) {
        return service.findByProduct(productId, rating, pageable);
    }

    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
    ResponseEntity<ProductReviewResponse> create(
            @PathVariable Long productId,
            Principal principal,
            @Valid @RequestBody CreateProductReviewRequest request) {
        ProductReviewResponse response = service.create(productId, principal.getName(), request);
        return ResponseEntity.ok(response);
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    ResponseEntity<ProductReviewResponse> createWithImages(
            @PathVariable Long productId,
            Principal principal,
            @Valid @RequestPart("review") CreateProductReviewRequest request,
            @RequestPart(value = "images", required = false) List<MultipartFile> images)
            throws IOException {
        ProductReviewResponse response =
                submissions.submit(
                        productId,
                        principal.getName(),
                        request,
                        images == null ? List.of() : images);
        return ResponseEntity.ok(response);
    }
}
