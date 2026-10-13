package com.project.shopapp.configuration;

import java.net.URI;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class VnpayProperties {
    private static final String HTTPS_SCHEME = "https";
    private static final String DEFAULT_PUBLIC_BASE_URL = "http://localhost:4200";
    private final boolean enabled;
    private final String tmnCode;
    private final String hashSecret;
    private final String publicBaseUrl;

    public VnpayProperties(
            @Value("${app.vnpay.enabled:false}") boolean enabled,
            @Value("${app.vnpay.tmn-code:}") String tmnCode,
            @Value("${app.vnpay.hash-secret:}") String hashSecret,
            @Value("${app.vnpay.public-base-url:" + DEFAULT_PUBLIC_BASE_URL + "}")
                    String publicBaseUrl) {
        this.enabled = enabled;
        this.tmnCode = tmnCode.trim();
        this.hashSecret = hashSecret.trim();
        this.publicBaseUrl = publicBaseUrl.trim().replaceAll("/+$", "");
        if (enabled) {
            URI uri = URI.create(this.publicBaseUrl);
            if (this.tmnCode.isBlank()
                    || this.hashSecret.isBlank()
                    || !HTTPS_SCHEME.equals(uri.getScheme())
                    || uri.getHost() == null
                    || uri.getRawQuery() != null
                    || uri.getRawFragment() != null
                    || uri.getUserInfo() != null
                    || !uri.getPath().isEmpty()) {
                throw new IllegalArgumentException(
                        "VNPAY requires merchant credentials and an HTTPS public origin");
            }
        }
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void requireEnabled() {
        if (!enabled) {
            throw new IllegalStateException("VNPAY Sandbox is not configured");
        }
    }

    public String getTmnCode() {
        return tmnCode;
    }

    public String getHashSecret() {
        return hashSecret;
    }

    public String getPublicBaseUrl() {
        return publicBaseUrl;
    }
}
