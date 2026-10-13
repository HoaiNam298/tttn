package com.project.shopapp.dtos.response;

import com.project.shopapp.enums.ShippingMethod;
import java.math.BigDecimal;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CheckoutQuoteResponse {

    private BigDecimal subtotal;

    private ShippingMethod shippingMethod;

    private BigDecimal shippingFee;

    private String voucherCode;

    private BigDecimal discount;

    private BigDecimal total;
}
