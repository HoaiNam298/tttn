package com.project.shopapp.controller;

import com.project.shopapp.dtos.request.CreateOrderRequest;
import com.project.shopapp.dtos.response.CheckoutQuoteResponse;
import com.project.shopapp.dtos.response.OrderResponse;
import com.project.shopapp.dtos.response.OrderSummaryResponse;
import com.project.shopapp.enums.OrderStatus;
import com.project.shopapp.services.OrderService;
import jakarta.validation.Valid;
import java.security.Principal;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/orders")
public class OrderController {
    private final OrderService service;

    public OrderController(OrderService service) {
        this.service = service;
    }

    @PostMapping
    ResponseEntity<OrderResponse> create(
            Principal principal, @Valid @RequestBody CreateOrderRequest request) {
        OrderResponse response = service.create(principal.getName(), request);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{id}")
    OrderResponse findById(Principal principal, @PathVariable Long id) {
        return service.findOwnedOrder(id, principal.getName());
    }

    @PostMapping("/quote")
    CheckoutQuoteResponse quote(
            Principal principal, @Valid @RequestBody CreateOrderRequest request) {
        return service.quote(principal.getName(), request);
    }

    @PostMapping("/{id}/cancel")
    OrderResponse cancel(Principal principal, @PathVariable Long id) {
        return service.cancelOwnedOrder(id, principal.getName());
    }

    @PostMapping("/{id}/confirm-receipt")
    OrderResponse confirmReceipt(Principal principal, @PathVariable Long id) {
        return service.confirmReceipt(id, principal.getName());
    }

    @GetMapping
    Page<OrderSummaryResponse> findMine(
            Principal principal,
            @RequestParam(required = false) OrderStatus status,
            @PageableDefault(size = 10, sort = "createdAt", direction = Sort.Direction.DESC)
                    Pageable pageable) {
        return service.findOwnedOrders(principal.getName(), status, pageable);
    }
}
