package com.project.shopapp.responses;

import org.springframework.data.domain.Page;

public record ProductReviewOverviewResponse(
        double averageRating, long totalReviews, Page<ProductReviewResponse> reviews) {}
