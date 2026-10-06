package com.project.shopapp.controller;

import com.project.shopapp.dtos.AddressRequest;
import com.project.shopapp.responses.AddressResponse;
import com.project.shopapp.services.AddressService;
import jakarta.validation.Valid;
import java.net.URI;
import java.security.Principal;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/addresses")
public class AddressController {
    private final AddressService service;

    public AddressController(AddressService service) {
        this.service = service;
    }

    @GetMapping
    public List<AddressResponse> findMine(Principal principal) {
        return service.findMine(principal.getName());
    }

    @PostMapping
    public ResponseEntity<AddressResponse> create(
            Principal principal, @Valid @RequestBody AddressRequest request) {
        AddressResponse response = service.save(principal.getName(), null, request);
        return ResponseEntity.created(URI.create("/api/v1/addresses/" + response.id()))
                .body(response);
    }

    @PutMapping("/{id}")
    public AddressResponse update(
            Principal principal,
            @PathVariable Long id,
            @Valid @RequestBody AddressRequest request) {
        return service.save(principal.getName(), id, request);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(Principal principal, @PathVariable Long id) {
        service.delete(principal.getName(), id);
        return ResponseEntity.noContent().build();
    }
}
