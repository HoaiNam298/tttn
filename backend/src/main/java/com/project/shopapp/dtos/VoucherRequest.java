package com.project.shopapp.dtos;

import com.project.shopapp.model.VoucherType;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import java.math.BigDecimal;
import java.time.Instant;

public record VoucherRequest(
        @NotBlank @Pattern(regexp = "^[A-Za-z0-9_-]{3,40}$") String code,
        @NotNull VoucherType type,
        @NotNull @DecimalMin("0.01") @Digits(integer = 10, fraction = 2) BigDecimal amount,
        @NotNull @DecimalMin("0") @Digits(integer = 12, fraction = 2) BigDecimal minimumSubtotal,
        @NotNull @DecimalMin("0.01") @Digits(integer = 10, fraction = 2) BigDecimal maximumDiscount,
        @NotNull Instant startsAt,
        @NotNull Instant endsAt,
        @NotNull @Min(1) @Max(1000000) Integer usageLimit,
        @NotNull @Min(1) @Max(1000000) Integer perUserLimit,
        @Min(1) Long targetUserId,
        boolean active,
        @Min(0) Long version) {}
