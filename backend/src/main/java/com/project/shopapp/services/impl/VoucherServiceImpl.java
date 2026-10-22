package com.project.shopapp.services.impl;

import com.project.shopapp.components.MessageKeys;
import com.project.shopapp.components.TransactionLog;
import com.project.shopapp.dtos.request.VoucherRequest;
import com.project.shopapp.dtos.response.VoucherResponse;
import com.project.shopapp.enums.OrderStatus;
import com.project.shopapp.exceptions.LocalizedArgumentException;
import com.project.shopapp.exceptions.LocalizedStateException;
import com.project.shopapp.exceptions.ResourceNotFoundException;
import com.project.shopapp.model.Voucher;
import com.project.shopapp.repositories.OrderRepository;
import com.project.shopapp.repositories.UserRepository;
import com.project.shopapp.repositories.VoucherRepository;
import com.project.shopapp.services.VoucherService;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.Locale;
import java.util.Set;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Slf4j
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
        var allowed = Set.of("id", "code", "createdAt", "startsAt", "endsAt");
        for (var order : pageable.getSort()) {
            if (!allowed.contains(order.getProperty())) {
                throw new LocalizedArgumentException(MessageKeys.VOUCHER_INVALID_SORT);
            }
        }
        var sort = pageable.getSort();
        if (sort.getOrderFor("id") == null) {
            sort = sort.and(Sort.by("id"));
        }
        Pageable bounded =
                PageRequest.of(
                        pageable.getPageNumber(), Math.min(pageable.getPageSize(), 100), sort);
        return vouchers.findAll(bounded).map(VoucherResponse::from);
    }

    @Transactional
    public VoucherResponse create(VoucherRequest request) {
        validateTarget(request.getTargetUserId());
        Voucher voucher = new Voucher(request);
        Voucher saved = vouchers.saveAndFlush(voucher);
        TransactionLog.info(log, "Voucher created voucherId={}", saved.getId());
        return VoucherResponse.from(saved);
    }

    @Transactional
    public VoucherResponse update(Long id, VoucherRequest request) {
        Voucher voucher =
                vouchers.findForUpdateById(id)
                        .orElseThrow(
                                () -> new ResourceNotFoundException(MessageKeys.VOUCHER_NOT_FOUND));
        if (request.getVersion() == null || request.getVersion() != voucher.getVersion()) {
            throw new LocalizedStateException(MessageKeys.VOUCHER_CHANGED);
        }
        validateTarget(request.getTargetUserId());
        voucher.update(request);
        Voucher saved = vouchers.saveAndFlush(voucher);
        TransactionLog.info(log, "Voucher updated voucherId={}", saved.getId());
        return VoucherResponse.from(saved);
    }

    public BigDecimal preview(String code, Long userId, BigDecimal subtotal) {
        Voucher voucher =
                vouchers.findByCode(normalize(code))
                        .orElseThrow(
                                () -> new ResourceNotFoundException(MessageKeys.VOUCHER_NOT_FOUND));
        return eligibleDiscount(voucher, userId, subtotal);
    }

    @Transactional
    public Voucher reserve(String code, Long userId, BigDecimal subtotal) {
        Voucher voucher =
                vouchers.findForUpdateByCode(normalize(code))
                        .orElseThrow(
                                () -> new ResourceNotFoundException(MessageKeys.VOUCHER_NOT_FOUND));
        eligibleDiscount(voucher, userId, subtotal);
        voucher.reserve();
        TransactionLog.debug(
                log, "Voucher reserved voucherId={} userId={}", voucher.getId(), userId);
        return voucher;
    }

    @Transactional
    public void release(Long id) {
        vouchers.findForUpdateById(id)
                .orElseThrow(() -> new ResourceNotFoundException(MessageKeys.VOUCHER_NOT_FOUND))
                .release();
        TransactionLog.debug(log, "Voucher released voucherId={}", id);
    }

    private BigDecimal eligibleDiscount(Voucher voucher, Long userId, BigDecimal subtotal) {
        long userUses =
                orders.countByVoucher_IdAndUser_IdAndStatusNot(
                        voucher.getId(), userId, OrderStatus.CANCELLED);
        return voucher.discountFor(userId, subtotal, userUses, Instant.now());
    }

    private void validateTarget(Long userId) {
        if (userId != null && !users.existsById(userId)) {
            throw new ResourceNotFoundException(MessageKeys.VOUCHER_TARGET_NOT_FOUND);
        }
    }

    private String normalize(String code) {
        return code.trim().toUpperCase(Locale.ROOT);
    }
}
