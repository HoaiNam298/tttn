package com.project.shopapp.components;

import com.project.shopapp.configuration.VnpayProperties;
import com.project.shopapp.exceptions.LocalizedArgumentException;
import java.math.BigDecimal;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.HexFormat;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Collectors;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.stereotype.Component;
import org.springframework.util.MultiValueMap;

@Component
public class VnpayGateway {
    private static final DateTimeFormatter DATE =
            DateTimeFormatter.ofPattern("yyyyMMddHHmmss").withZone(ZoneId.of("Asia/Ho_Chi_Minh"));
    private static final String PAYMENT_URL = "https://sandbox.vnpayment.vn/paymentv2/vpcpay.html";
    private final VnpayProperties properties;

    public VnpayGateway(VnpayProperties properties) {
        this.properties = properties;
    }

    public static long amount(BigDecimal total) {
        long amount;
        try {
            amount = total.movePointRight(2).longValueExact();
        } catch (ArithmeticException exception) {
            throw new LocalizedArgumentException(MessageKeys.PAYMENT_INVALID_AMOUNT, exception);
        }
        if (total.stripTrailingZeros().scale() > 0 || amount <= 0 || amount > 999999999999L) {
            throw new LocalizedArgumentException(MessageKeys.PAYMENT_AMOUNT_LIMIT);
        }
        return amount;
    }

    public String createUrl(
            String ref,
            Long orderId,
            BigDecimal total,
            Instant createdAt,
            Instant expiresAt,
            String ip) {
        properties.requireEnabled();
        Map<String, String> params = new TreeMap<>();
        params.put("vnp_Version", "2.1.0");
        params.put("vnp_Command", "pay");
        params.put("vnp_TmnCode", properties.getTmnCode());
        params.put("vnp_Amount", Long.toString(amount(total)));
        params.put("vnp_CurrCode", "VND");
        params.put("vnp_Locale", "vn");
        params.put("vnp_TxnRef", ref);
        params.put("vnp_OrderInfo", "Thanh toan don hang " + orderId);
        params.put("vnp_OrderType", "other");
        params.put("vnp_CreateDate", DATE.format(createdAt));
        params.put("vnp_ExpireDate", DATE.format(expiresAt));
        params.put("vnp_IpAddr", ip);
        params.put(
                "vnp_ReturnUrl", properties.getPublicBaseUrl() + "/api/v1/payments/vnpay/return");
        String query = canonical(params);
        return PAYMENT_URL + "?" + query + "&vnp_SecureHash=" + sign(params);
    }

    public boolean verify(MultiValueMap<String, String> params) {
        if (!properties.isEnabled()) {
            return false;
        }
        for (var entry : params.entrySet()) {
            if (entry.getKey().startsWith("vnp_") && entry.getValue().size() != 1) {
                return false;
            }
        }
        String signature = params.getFirst("vnp_SecureHash");
        if (signature == null
                || !signature.matches("[a-fA-F0-9]{128}")
                || !properties.getTmnCode().equals(params.getFirst("vnp_TmnCode"))) {
            return false;
        }
        return MessageDigest.isEqual(
                HexFormat.of().parseHex(signature),
                HexFormat.of().parseHex(sign(params.toSingleValueMap())));
    }

    public String sign(Map<String, String> params) {
        try {
            Mac mac = Mac.getInstance("HmacSHA512");
            byte[] secret = properties.getHashSecret().getBytes(StandardCharsets.UTF_8);
            mac.init(new SecretKeySpec(secret, "HmacSHA512"));
            byte[] payload = canonical(params).getBytes(StandardCharsets.UTF_8);
            return HexFormat.of().formatHex(mac.doFinal(payload));
        } catch (GeneralSecurityException exception) {
            throw new IllegalStateException("Cannot sign VNPAY request", exception);
        }
    }

    private String canonical(Map<String, String> params) {
        Map<String, String> sortedParams = new TreeMap<>(params);
        return sortedParams.entrySet().stream()
                .filter(this::isSignableParameter)
                .map(this::encodeParameter)
                .collect(Collectors.joining("&"));
    }

    private boolean isSignableParameter(Map.Entry<String, String> entry) {
        String key = entry.getKey();
        return key.startsWith("vnp_")
                && !key.equals("vnp_SecureHash")
                && !key.equals("vnp_SecureHashType")
                && entry.getValue() != null
                && !entry.getValue().isEmpty();
    }

    private String encodeParameter(Map.Entry<String, String> entry) {
        return encode(entry.getKey()) + "=" + encode(entry.getValue());
    }

    private String encode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }
}
