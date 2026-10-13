package com.project.shopapp.dtos.response;

import com.project.shopapp.enums.VoucherType;
import com.project.shopapp.model.Voucher;
import java.math.BigDecimal;
import java.time.Instant;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VoucherResponse {

    private Long id;

    private String code;

    private VoucherType type;

    private BigDecimal amount;

    private BigDecimal minimumSubtotal;

    private BigDecimal maximumDiscount;

    private Instant startsAt;

    private Instant endsAt;

    private int usageLimit;

    private int perUserLimit;

    private int usedCount;

    private Long targetUserId;

    private boolean active;

    private long version;

    public static VoucherResponse from(Voucher voucher) {
        return VoucherResponse.builder()
                .id(voucher.getId())
                .code(voucher.getCode())
                .type(voucher.getType())
                .amount(voucher.getAmount())
                .minimumSubtotal(voucher.getMinimumSubtotal())
                .maximumDiscount(voucher.getMaximumDiscount())
                .startsAt(voucher.getStartsAt())
                .endsAt(voucher.getEndsAt())
                .usageLimit(voucher.getUsageLimit())
                .perUserLimit(voucher.getPerUserLimit())
                .usedCount(voucher.getUsedCount())
                .targetUserId(voucher.getTargetUserId())
                .active(voucher.isActive())
                .version(voucher.getVersion())
                .build();
    }
}
