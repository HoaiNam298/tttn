package com.project.shopapp.controller;

import com.project.shopapp.responses.FavoriteStateResponse;
import com.project.shopapp.responses.ProductResponse;
import com.project.shopapp.services.FavoriteService;
import java.security.Principal;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/favorites")
public class FavoriteController {
    private final FavoriteService service;

    public FavoriteController(FavoriteService service) {
        this.service = service;
    }

    @GetMapping
    Page<ProductResponse> findAll(
            Principal principal, @PageableDefault(size = 12) Pageable pageable) {
        return service.findOwned(principal.getName(), pageable);
    }

    @GetMapping("/{productId}")
    FavoriteStateResponse state(Principal principal, @PathVariable Long productId) {
        return new FavoriteStateResponse(service.isFavorite(principal.getName(), productId));
    }

    @PutMapping("/{productId}")
    ResponseEntity<Void> add(Principal principal, @PathVariable Long productId) {
        service.add(principal.getName(), productId);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{productId}")
    ResponseEntity<Void> remove(Principal principal, @PathVariable Long productId) {
        service.remove(principal.getName(), productId);
        return ResponseEntity.noContent().build();
    }
}
