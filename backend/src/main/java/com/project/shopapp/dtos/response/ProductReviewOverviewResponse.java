package com.project.shopapp.dtos.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.domain.Page;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProductReviewOverviewResponse {

    private double averageRating;

    private long totalReviews;

    private Page<ProductReviewResponse> reviews;
}
