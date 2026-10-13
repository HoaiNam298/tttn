package com.project.shopapp.enums;

import java.math.BigDecimal;
import lombok.Getter;

public enum ShippingMethod {
    STANDARD("30000.00"),
    EXPRESS("50000.00");

    @Getter private final BigDecimal fee;

    ShippingMethod(String fee) {
        this.fee = new BigDecimal(fee);
    }
}
