package com.project.shopapp.projections;

import java.math.BigDecimal;

public interface TopProductProjection {
    Long getProductId();

    String getName();

    long getQuantity();

    BigDecimal getGross();
}
