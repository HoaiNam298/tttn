package com.project.shopapp.components;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.project.shopapp.configuration.VnpayProperties;
import java.math.BigDecimal;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import org.junit.jupiter.api.Test;
import org.springframework.util.LinkedMultiValueMap;

class VnpayGatewayTest {
    private final VnpayGateway gateway =
            new VnpayGateway(
                    new VnpayProperties(
                            true, "TESTCODE", "sandbox-test-secret", "https://demo.example.com"));

    @Test
    void urlUsesSandboxVndAmountAndVietnamTimezone() {
        Instant start = Instant.parse("2026-10-12T01:00:00Z");
        String url =
                gateway.createUrl(
                        "reference",
                        1L,
                        new BigDecimal("10000.00"),
                        start,
                        start.plusSeconds(900),
                        "127.0.0.1");
        assertTrue(url.startsWith("https://sandbox.vnpayment.vn/paymentv2/vpcpay.html?"));
        assertTrue(url.contains("vnp_Amount=1000000"));
        assertTrue(url.contains("vnp_CreateDate=20261012080000"));
        assertTrue(url.contains("vnp_ExpireDate=20261012081500"));
        var params = new LinkedMultiValueMap<String, String>();
        for (String field : url.substring(url.indexOf('?') + 1).split("&")) {
            String[] pair = field.split("=", 2);
            params.add(pair[0], URLDecoder.decode(pair[1], StandardCharsets.UTF_8));
        }
        assertTrue(gateway.verify(params));
        params.add("vnp_Amount", "1");
        assertFalse(gateway.verify(params));
    }

    @Test
    void wrongMerchantAndUnsignedDataAreRejected() {
        var params = new LinkedMultiValueMap<String, String>();
        params.set("vnp_TmnCode", "OTHER");
        params.set("vnp_SecureHash", gateway.sign(params.toSingleValueMap()));
        assertFalse(gateway.verify(params));
        params.clear();
        assertFalse(gateway.verify(params));
    }

    @Test
    void amountRejectsFractionsZeroAndOverflow() {
        assertEquals(1000000L, VnpayGateway.amount(new BigDecimal("10000.00")));
        assertThrows(
                IllegalArgumentException.class,
                () -> VnpayGateway.amount(new BigDecimal("10000.01")));
        assertThrows(IllegalArgumentException.class, () -> VnpayGateway.amount(BigDecimal.ZERO));
        assertThrows(
                IllegalArgumentException.class,
                () -> VnpayGateway.amount(new BigDecimal("100000000000000000000")));
    }

    @Test
    void enabledConfigurationRequiresCredentialsAndHttps() {
        assertThrows(
                IllegalArgumentException.class,
                () -> new VnpayProperties(true, "", "", "https://demo.example.com"));
        assertThrows(
                IllegalArgumentException.class,
                () -> new VnpayProperties(true, "TESTCODE", "test", "http://localhost:4200"));
        assertThrows(
                IllegalArgumentException.class,
                () ->
                        new VnpayProperties(
                                true, "TESTCODE", "test", "https://demo.example.com/redirect"));
        assertFalse(new VnpayProperties(false, "", "", "http://localhost:4200").isEnabled());
    }
}
