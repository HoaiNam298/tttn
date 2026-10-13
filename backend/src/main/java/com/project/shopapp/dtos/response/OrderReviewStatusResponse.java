package com.project.shopapp.dtos.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderReviewStatusResponse {

    private Long productId;

    private Long variantId;

    private boolean reviewed;
}
