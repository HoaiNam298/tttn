package com.project.shopapp.services;

import com.project.shopapp.responses.ProductResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface FavoriteService {
    Page<ProductResponse> findOwned(String phoneNumber, Pageable pageable);

    boolean isFavorite(String phoneNumber, Long productId);

    void add(String phoneNumber, Long productId);

    void remove(String phoneNumber, Long productId);
}
