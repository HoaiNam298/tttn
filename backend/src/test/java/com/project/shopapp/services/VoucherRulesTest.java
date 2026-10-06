package com.project.shopapp.services;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.project.shopapp.dtos.VoucherRequest;
import com.project.shopapp.model.Voucher;
import com.project.shopapp.model.VoucherType;
import java.math.BigDecimal;
import java.time.Instant;
import org.junit.jupiter.api.Test;

class VoucherRulesTest {
    private final Instant now = Instant.parse("2026-10-06T00:00:00Z");

    private VoucherRequest request(VoucherType type, String amount, Long target) {
        return new VoucherRequest(
                "SAVE10",
                type,
                new BigDecimal(amount),
                new BigDecimal("100"),
                new BigDecimal("25"),
                now.minusSeconds(60),
                now.plusSeconds(60),
                1,
                1,
                target,
                true,
                null);
    }

    @Test
    void capsPercentageDiscountAndDoesNotExceedSubtotal() {
        Voucher voucher = new Voucher(request(VoucherType.PERCENT, "10", null));
        assertEquals(new BigDecimal("25"), voucher.discountFor(1L, new BigDecimal("500"), 0, now));
        Voucher fixed = new Voucher(request(VoucherType.FIXED, "200", null));
        assertEquals(new BigDecimal("25"), fixed.discountFor(1L, new BigDecimal("100"), 0, now));
    }

    @Test
    void rejectsExpiredFutureDisabledAndMinimumSubtotal() {
        Voucher voucher = new Voucher(request(VoucherType.FIXED, "10", null));
        assertThrows(
                IllegalStateException.class,
                () -> voucher.discountFor(1L, new BigDecimal("100"), 0, now.plusSeconds(60)));
        assertThrows(
                IllegalStateException.class,
                () -> voucher.discountFor(1L, new BigDecimal("100"), 0, now.minusSeconds(61)));
        assertThrows(
                IllegalStateException.class,
                () -> voucher.discountFor(1L, new BigDecimal("99"), 0, now));
        VoucherRequest input = request(VoucherType.FIXED, "10", null);
        voucher.update(
                new VoucherRequest(
                        input.code(),
                        input.type(),
                        input.amount(),
                        input.minimumSubtotal(),
                        input.maximumDiscount(),
                        input.startsAt(),
                        input.endsAt(),
                        input.usageLimit(),
                        input.perUserLimit(),
                        null,
                        false,
                        null));
        assertThrows(
                IllegalStateException.class,
                () -> voucher.discountFor(1L, new BigDecimal("100"), 0, now));
    }

    @Test
    void enforcesTargetAndPerUserLimit() {
        Voucher voucher = new Voucher(request(VoucherType.FIXED, "10", 7L));
        assertThrows(
                IllegalStateException.class,
                () -> voucher.discountFor(8L, new BigDecimal("100"), 0, now));
        assertThrows(
                IllegalStateException.class,
                () -> voucher.discountFor(7L, new BigDecimal("100"), 1, now));
        assertEquals(new BigDecimal("10"), voucher.discountFor(7L, new BigDecimal("100"), 0, now));
    }

    @Test
    void reservesLastUseAndReleasesExactlyOnce() {
        Voucher voucher = new Voucher(request(VoucherType.FIXED, "10", null));
        voucher.reserve();
        assertEquals(new BigDecimal("10"), voucher.calculateDiscount(new BigDecimal("100")));
        assertThrows(IllegalStateException.class, voucher::reserve);
        voucher.release();
        assertEquals(0, voucher.getUsedCount());
        assertThrows(IllegalStateException.class, voucher::release);
    }

    @Test
    void rejectsInvalidDatesPercentageAndChangingCode() {
        assertThrows(
                IllegalArgumentException.class,
                () -> new Voucher(request(VoucherType.PERCENT, "101", null)));
        Voucher voucher = new Voucher(request(VoucherType.FIXED, "10", null));
        VoucherRequest input = request(VoucherType.FIXED, "10", null);
        assertThrows(
                IllegalArgumentException.class,
                () ->
                        voucher.update(
                                new VoucherRequest(
                                        "OTHER",
                                        input.type(),
                                        input.amount(),
                                        input.minimumSubtotal(),
                                        input.maximumDiscount(),
                                        input.startsAt(),
                                        input.endsAt(),
                                        1,
                                        1,
                                        null,
                                        true,
                                        null)));
        assertThrows(
                IllegalArgumentException.class,
                () ->
                        new Voucher(
                                new VoucherRequest(
                                        "SAVE10",
                                        input.type(),
                                        input.amount(),
                                        input.minimumSubtotal(),
                                        input.maximumDiscount(),
                                        now,
                                        now,
                                        1,
                                        1,
                                        null,
                                        true,
                                        null)));
    }
}
