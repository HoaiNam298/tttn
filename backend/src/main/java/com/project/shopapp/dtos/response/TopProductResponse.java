package com.project.shopapp.dtos.response;

import java.math.BigDecimal;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TopProductResponse {

    private Long productId;

    private String productName;

    private long quantity;

    private BigDecimal grossSales;
}
