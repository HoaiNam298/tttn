package com.project.shopapp.services;

import com.project.shopapp.dtos.request.VoucherRequest;
import com.project.shopapp.dtos.response.VoucherResponse;
import com.project.shopapp.model.Voucher;
import java.math.BigDecimal;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface VoucherService {
    Page<VoucherResponse> findAll(Pageable pageable);

    VoucherResponse create(VoucherRequest request);

    VoucherResponse update(Long id, VoucherRequest request);

    BigDecimal preview(String code, Long userId, BigDecimal subtotal);

    Voucher reserve(String code, Long userId, BigDecimal subtotal);

    void release(Long id);
}
