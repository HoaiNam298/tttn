package com.project.shopapp.dtos.response;

import com.project.shopapp.model.ProductReview;
import java.time.Instant;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProductReviewResponse {

    private Long id;

    private String reviewerName;

    private int rating;

    private String comment;

    private Instant createdAt;

    private List<String> images;

    private String shopReply;

    private Instant repliedAt;

    private long version;

    private Long productId;

    private String productName;

    public static ProductReviewResponse from(ProductReview review) {
        return ProductReviewResponse.builder()
                .id(review.getId())
                .reviewerName(review.getUser().getFullName())
                .rating(review.getRating())
                .comment(review.getComment())
                .createdAt(review.getCreatedAt())
                .images(review.getImages())
                .shopReply(review.getShopReply())
                .repliedAt(review.getRepliedAt())
                .version(review.getVersion())
                .productId(review.getProduct().getId())
                .productName(review.getProduct().getName())
                .build();
    }
}
