package com.project.shopapp.controller;

import com.project.shopapp.dtos.CreateOrderRequest;
import com.project.shopapp.responses.OrderResponse;
import com.project.shopapp.services.OrderService;
import jakarta.validation.Valid;
import java.net.URI;
import java.security.Principal;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
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
        return ResponseEntity.created(URI.create("/api/v1/orders/" + response.id())).body(response);
    }

    @GetMapping("/{id}")
    OrderResponse findById(Principal principal, @PathVariable Long id) {
        return service.findOwnedOrder(id, principal.getName());
    }
}
