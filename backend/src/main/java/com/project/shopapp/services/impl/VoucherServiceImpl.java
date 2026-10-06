package com.project.shopapp.services.impl;

import com.project.shopapp.dtos.VoucherRequest;
import com.project.shopapp.exceptions.ResourceNotFoundException;
import com.project.shopapp.model.OrderStatus;
import com.project.shopapp.model.Voucher;
import com.project.shopapp.repositories.OrderRepository;
import com.project.shopapp.repositories.UserRepository;
import com.project.shopapp.repositories.VoucherRepository;
import com.project.shopapp.responses.VoucherResponse;
import com.project.shopapp.services.VoucherService;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.Locale;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class VoucherServiceImpl implements VoucherService {
    private final VoucherRepository vouchers;
    private final OrderRepository orders;
    private final UserRepository users;

    public VoucherServiceImpl(
            VoucherRepository vouchers, OrderRepository orders, UserRepository users) {
        this.vouchers = vouchers;
        this.orders = orders;
        this.users = users;
    }

    public Page<VoucherResponse> findAll(Pageable pageable) {
        var allowed = java.util.Set.of("id", "code", "createdAt", "startsAt", "endsAt");
        for (var order : pageable.getSort()) {
            if (!allowed.contains(order.getProperty())) {
                throw new IllegalArgumentException("Unsupported voucher sort field");
            }
        }
        var sort = pageable.getSort();
        if (sort.getOrderFor("id") == null) {
            sort = sort.and(org.springframework.data.domain.Sort.by("id"));
        }
        return vouchers.findAll(
                        org.springframework.data.domain.PageRequest.of(
                                pageable.getPageNumber(),
                                Math.min(pageable.getPageSize(), 100),
                                sort))
                .map(VoucherResponse::from);
    }

    @Transactional
    public VoucherResponse create(VoucherRequest request) {
        validateTarget(request.targetUserId());
        return VoucherResponse.from(vouchers.saveAndFlush(new Voucher(request)));
    }

    @Transactional
    public VoucherResponse update(Long id, VoucherRequest request) {
        Voucher voucher =
                vouchers.findForUpdateById(id)
                        .orElseThrow(() -> new ResourceNotFoundException("Voucher not found"));
        if (request.version() == null || request.version() != voucher.getVersion()) {
            throw new IllegalStateException("Voucher changed. Reload before saving");
        }
        validateTarget(request.targetUserId());
        voucher.update(request);
        return VoucherResponse.from(vouchers.saveAndFlush(voucher));
    }

    public BigDecimal preview(String code, Long userId, BigDecimal subtotal) {
        Voucher voucher =
                vouchers.findByCode(normalize(code))
                        .orElseThrow(() -> new ResourceNotFoundException("Voucher not found"));
        return eligibleDiscount(voucher, userId, subtotal);
    }

    @Transactional
    public Voucher reserve(String code, Long userId, BigDecimal subtotal) {
        Voucher voucher =
                vouchers.findForUpdateByCode(normalize(code))
                        .orElseThrow(() -> new ResourceNotFoundException("Voucher not found"));
        eligibleDiscount(voucher, userId, subtotal);
        voucher.reserve();
        return voucher;
    }

    @Transactional
    public void release(Long id) {
        vouchers.findForUpdateById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Voucher not found"))
                .release();
    }

    private BigDecimal eligibleDiscount(Voucher voucher, Long userId, BigDecimal subtotal) {
        long userUses =
                orders.countByVoucher_IdAndUser_IdAndStatusNot(
                        voucher.getId(), userId, OrderStatus.CANCELLED);
        return voucher.discountFor(userId, subtotal, userUses, Instant.now());
    }

    private void validateTarget(Long userId) {
        if (userId != null && !users.existsById(userId)) {
            throw new ResourceNotFoundException("Target user not found");
        }
    }

    private String normalize(String code) {
        return code.trim().toUpperCase(Locale.ROOT);
    }
}
