package com.project.shopapp.model;

import com.project.shopapp.dtos.VoucherRequest;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.Locale;

@Entity
@Table(name = "vouchers")
public class Voucher extends BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 40)
    private String code;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private VoucherType type;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal amount;

    @Column(name = "minimum_subtotal", nullable = false, precision = 14, scale = 2)
    private BigDecimal minimumSubtotal;

    @Column(name = "maximum_discount", nullable = false, precision = 12, scale = 2)
    private BigDecimal maximumDiscount;

    @Column(name = "starts_at", nullable = false)
    private Instant startsAt;

    @Column(name = "ends_at", nullable = false)
    private Instant endsAt;

    @Column(name = "usage_limit", nullable = false)
    private int usageLimit;

    @Column(name = "per_user_limit", nullable = false)
    private int perUserLimit;

    @Column(name = "used_count", nullable = false)
    private int usedCount;

    @Column(name = "target_user_id")
    private Long targetUserId;

    @Column(nullable = false)
    private boolean active;

    @Version
    @Column(nullable = false)
    private long version;

    protected Voucher() {}

    public Voucher(VoucherRequest request) {
        update(request);
    }

    public void update(VoucherRequest request) {
        if (!request.endsAt().isAfter(request.startsAt())
                || (request.type() == VoucherType.PERCENT
                        && request.amount().compareTo(BigDecimal.valueOf(100)) > 0)
                || request.usageLimit() < usedCount) {
            throw new IllegalArgumentException("Invalid voucher dates, percentage or usage limit");
        }
        String normalizedCode = request.code().trim().toUpperCase(Locale.ROOT);
        if (code != null && !code.equals(normalizedCode)) {
            throw new IllegalArgumentException("Voucher code cannot be changed");
        }
        code = normalizedCode;
        type = request.type();
        amount = request.amount();
        minimumSubtotal = request.minimumSubtotal();
        maximumDiscount = request.maximumDiscount();
        startsAt = request.startsAt();
        endsAt = request.endsAt();
        usageLimit = request.usageLimit();
        perUserLimit = request.perUserLimit();
        targetUserId = request.targetUserId();
        active = request.active();
    }

    public BigDecimal discountFor(Long userId, BigDecimal subtotal, long userUses, Instant now) {
        if (!active
                || now.isBefore(startsAt)
                || !now.isBefore(endsAt)
                || usedCount >= usageLimit
                || userUses >= perUserLimit
                || (targetUserId != null && !targetUserId.equals(userId))
                || subtotal.compareTo(minimumSubtotal) < 0) {
            throw new IllegalStateException("Voucher is unavailable or this order is not eligible");
        }
        return calculateDiscount(subtotal);
    }

    public BigDecimal calculateDiscount(BigDecimal subtotal) {
        BigDecimal discount =
                type == VoucherType.FIXED
                        ? amount
                        : subtotal.multiply(amount)
                                .divide(BigDecimal.valueOf(100), 2, RoundingMode.DOWN);
        return discount.min(maximumDiscount).min(subtotal);
    }

    public void reserve() {
        if (usedCount >= usageLimit) {
            throw new IllegalStateException("Voucher usage limit reached");
        }
        usedCount++;
    }

    public void release() {
        if (usedCount < 1) {
            throw new IllegalStateException("Invalid voucher usage count");
        }
        usedCount--;
    }

    public Long getId() {
        return id;
    }

    public String getCode() {
        return code;
    }

    public VoucherType getType() {
        return type;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public BigDecimal getMinimumSubtotal() {
        return minimumSubtotal;
    }

    public BigDecimal getMaximumDiscount() {
        return maximumDiscount;
    }

    public Instant getStartsAt() {
        return startsAt;
    }

    public Instant getEndsAt() {
        return endsAt;
    }

    public int getUsageLimit() {
        return usageLimit;
    }

    public int getPerUserLimit() {
        return perUserLimit;
    }

    public int getUsedCount() {
        return usedCount;
    }

    public Long getTargetUserId() {
        return targetUserId;
    }

    public boolean isActive() {
        return active;
    }

    public long getVersion() {
        return version;
    }
}
