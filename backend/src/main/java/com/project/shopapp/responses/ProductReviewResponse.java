package com.project.shopapp.responses;

import com.project.shopapp.model.ProductReview;
import java.time.Instant;

public record ProductReviewResponse(
        Long id, String reviewerName, int rating, String comment, Instant createdAt) {
    public static ProductReviewResponse from(ProductReview review) {
        return new ProductReviewResponse(
                review.getId(),
                review.getUser().getFullName(),
                review.getRating(),
                review.getComment(),
                review.getCreatedAt());
    }
}
