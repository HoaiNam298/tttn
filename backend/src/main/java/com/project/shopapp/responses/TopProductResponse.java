package com.project.shopapp.responses;

import java.math.BigDecimal;

public record TopProductResponse(
        Long productId, String productName, long quantity, BigDecimal grossSales) {}
