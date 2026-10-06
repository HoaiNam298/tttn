package com.project.shopapp.responses;

import com.project.shopapp.model.Voucher;
import com.project.shopapp.model.VoucherType;
import java.math.BigDecimal;
import java.time.Instant;

public record VoucherResponse(
        Long id,
        String code,
        VoucherType type,
        BigDecimal amount,
        BigDecimal minimumSubtotal,
        BigDecimal maximumDiscount,
        Instant startsAt,
        Instant endsAt,
        int usageLimit,
        int perUserLimit,
        int usedCount,
        Long targetUserId,
        boolean active,
        long version) {
    public static VoucherResponse from(Voucher voucher) {
        return new VoucherResponse(
                voucher.getId(),
                voucher.getCode(),
                voucher.getType(),
                voucher.getAmount(),
                voucher.getMinimumSubtotal(),
                voucher.getMaximumDiscount(),
                voucher.getStartsAt(),
                voucher.getEndsAt(),
                voucher.getUsageLimit(),
                voucher.getPerUserLimit(),
                voucher.getUsedCount(),
                voucher.getTargetUserId(),
                voucher.isActive(),
                voucher.getVersion());
    }
}
