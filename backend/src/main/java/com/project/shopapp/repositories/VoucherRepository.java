package com.project.shopapp.repositories;

import com.project.shopapp.model.Voucher;
import jakarta.persistence.LockModeType;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;

public interface VoucherRepository extends JpaRepository<Voucher, Long> {
    Optional<Voucher> findByCode(String code);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<Voucher> findForUpdateByCode(String code);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<Voucher> findForUpdateById(Long id);
}
