package com.project.shopapp.responses;

import java.time.Instant;

public record PaymentLinkResponse(Long orderId, String paymentUrl, Instant expiresAt) {}
