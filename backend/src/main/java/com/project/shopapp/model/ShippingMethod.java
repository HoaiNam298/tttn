package com.project.shopapp.model;

import java.math.BigDecimal;

public enum ShippingMethod {
    STANDARD("30000.00"),
    EXPRESS("50000.00");

    private final BigDecimal fee;

    ShippingMethod(String fee) {
        this.fee = new BigDecimal(fee);
    }

    public BigDecimal getFee() {
        return fee;
    }
}
