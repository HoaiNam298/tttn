package com.project.shopapp.responses;

public record OrderReviewStatusResponse(Long productId, Long variantId, boolean reviewed) {}
