package com.project.shopapp.controller;

import com.project.shopapp.responses.OrderReviewStatusResponse;
import com.project.shopapp.services.ProductReviewService;
import java.security.Principal;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
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
    List<OrderReviewStatusResponse> statuses(Principal principal, @PathVariable Long orderId) {
        return service.orderReviewStatuses(principal.getName(), orderId);
    }
}
