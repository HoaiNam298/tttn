package com.project.shopapp.repositories;

import com.project.shopapp.model.UserAddress;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserAddressRepository extends JpaRepository<UserAddress, Long> {
    List<UserAddress> findAllByUserIdOrderByIdAsc(Long userId);
}
