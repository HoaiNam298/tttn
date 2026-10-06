package com.project.shopapp.controller;

import com.project.shopapp.dtos.VoucherRequest;
import com.project.shopapp.responses.VoucherResponse;
import com.project.shopapp.services.VoucherService;
import jakarta.validation.Valid;
import java.net.URI;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/admin/vouchers")
public class VoucherController {
    private final VoucherService service;

    public VoucherController(VoucherService service) {
        this.service = service;
    }

    @GetMapping
    Page<VoucherResponse> findAll(
            @PageableDefault(size = 10, sort = "id", direction = Sort.Direction.DESC)
                    Pageable pageable) {
        return service.findAll(pageable);
    }

    @PostMapping
    ResponseEntity<VoucherResponse> create(@Valid @RequestBody VoucherRequest request) {
        VoucherResponse response = service.create(request);
        return ResponseEntity.created(URI.create("/api/v1/admin/vouchers/" + response.id()))
                .body(response);
    }

    @PutMapping("/{id}")
    VoucherResponse update(@PathVariable Long id, @Valid @RequestBody VoucherRequest request) {
        return service.update(id, request);
    }
}
