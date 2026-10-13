package com.project.shopapp.dtos.response;

import java.time.Instant;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentLinkResponse {

    private Long orderId;

    private String paymentUrl;

    private Instant expiresAt;
}
