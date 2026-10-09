package com.project.shopapp.controller;

import com.project.shopapp.services.ProductReviewService;
import java.security.Principal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class ReviewEligibilityController {
    private final ProductReviewService service;

    public ReviewEligibilityController(ProductReviewService service) {
        this.service = service;
    }

    @GetMapping("/api/v1/review-eligibility")
    boolean reviewed(
            Principal principal,
            @RequestParam Long productId,
            @RequestParam Long orderId,
            @RequestParam(required = false) Long variantId) {
        return service.reviewed(productId, principal.getName(), orderId, variantId);
    }

    @GetMapping("/api/v1/orders/{orderId}/review-statuses")
    java.util.List<com.project.shopapp.responses.OrderReviewStatusResponse> statuses(
            Principal principal,
            @org.springframework.web.bind.annotation.PathVariable Long orderId) {
        return service.orderReviewStatuses(principal.getName(), orderId);
    }
}
