package com.project.shopapp.controller;

import com.project.shopapp.configuration.VnpayProperties;
import com.project.shopapp.dtos.response.PaymentLinkResponse;
import com.project.shopapp.dtos.response.VnpayIpnResponse;
import com.project.shopapp.services.PaymentService;
import jakarta.servlet.http.HttpServletRequest;
import java.net.URI;
import java.security.Principal;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/payments")
@Slf4j
public class PaymentController {
    private final PaymentService service;
    private final VnpayProperties properties;

    public PaymentController(PaymentService service, VnpayProperties properties) {
        this.service = service;
        this.properties = properties;
    }

    @GetMapping("/methods")
    public Map<String, Boolean> methods() {
        return Map.of("vnpayEnabled", properties.isEnabled());
    }

    @PostMapping("/vnpay/orders/{orderId}")
    public PaymentLinkResponse create(
            @PathVariable Long orderId, Principal principal, HttpServletRequest request) {
        return service.createLink(orderId, principal.getName(), request.getRemoteAddr());
    }

    @GetMapping("/vnpay/ipn")
    public VnpayIpnResponse ipn(@RequestParam MultiValueMap<String, String> params) {
        try {
            return service.receiveIpn(params);
        } catch (RuntimeException exception) {
            log.error("VNPAY IPN processing failed", exception);
            return new VnpayIpnResponse("99", "Processing failed. Retry notification");
        }
    }

    @GetMapping("/vnpay/return")
    public ResponseEntity<Void> returned(@RequestParam MultiValueMap<String, String> params) {
        return ResponseEntity.status(302)
                .location(URI.create(service.returnLocation(params)))
                .build();
    }
}
