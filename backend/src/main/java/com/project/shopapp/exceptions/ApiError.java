package com.project.shopapp.exceptions;

import java.time.Instant;
import java.util.Locale;
import java.util.Map;
import org.springframework.http.HttpStatusCode;

public record ApiError(
        Instant timestamp, int status, String code, String message, Map<String, String> errors) {
    public static ApiError of(
            HttpStatusCode status, String key, String message, Map<String, String> errors) {
        String code =
                switch (key) {
                    case "request.invalid",
                                    "request.invalid_argument",
                                    "request.invalid_parameter" ->
                            "INVALID_REQUEST";
                    case "error.internal" -> "INTERNAL_ERROR";
                    case "security.access_denied" -> "ACCESS_DENIED";
                    case "security.authentication_required" -> "AUTHENTICATION_REQUIRED";
                    case "security.authentication_unavailable" -> "AUTHENTICATION_UNAVAILABLE";
                    case "request.invalid_state" -> "INVALID_STATE";
                    case "stock.insufficient" -> "INSUFFICIENT_STOCK";
                    default -> key.replace('.', '_').toUpperCase(Locale.ROOT);
                };
        return new ApiError(Instant.now(), status.value(), code, message, errors);
    }
}
