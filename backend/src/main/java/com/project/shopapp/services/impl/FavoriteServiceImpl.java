package com.project.shopapp.services.impl;

import com.project.shopapp.components.MessageKeys;
import com.project.shopapp.components.TransactionLog;
import com.project.shopapp.dtos.response.ProductResponse;
import com.project.shopapp.exceptions.ResourceNotFoundException;
import com.project.shopapp.model.Favorite;
import com.project.shopapp.model.Product;
import com.project.shopapp.model.User;
import com.project.shopapp.repositories.FavoriteRepository;
import com.project.shopapp.repositories.ProductRepository;
import com.project.shopapp.repositories.UserRepository;
import com.project.shopapp.services.FavoriteService;
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
public class FavoriteServiceImpl implements FavoriteService {
    private final FavoriteRepository favorites;
    private final UserRepository users;
    private final ProductRepository products;

    public FavoriteServiceImpl(
            FavoriteRepository favorites, UserRepository users, ProductRepository products) {
        this.favorites = favorites;
        this.users = users;
        this.products = products;
    }

    @Override
    public Page<ProductResponse> findOwned(String phoneNumber, Pageable pageable) {
        Pageable bounded =
                PageRequest.of(
                        pageable.getPageNumber(),
                        Math.min(pageable.getPageSize(), 50),
                        Sort.by(Sort.Direction.DESC, "id"));
        Page<Favorite> page = favorites.findByUserPhoneNumber(phoneNumber, bounded);
        return page.map(favorite -> ProductResponse.from(favorite.getProduct()));
    }

    @Override
    public boolean isFavorite(String phoneNumber, Long productId) {
        return favorites.existsByUserPhoneNumberAndProductId(phoneNumber, productId);
    }

    @Override
    @Transactional
    public void add(String phoneNumber, Long productId) {
        User user = lockUser(phoneNumber);
        if (!favorites.existsByUserIdAndProductId(user.getId(), productId)) {
            Product product =
                    products.findById(productId)
                            .orElseThrow(
                                    () ->
                                            new ResourceNotFoundException(
                                                    MessageKeys.PRODUCT_NOT_FOUND));
            Favorite favorite = Favorite.builder().user(user).product(product).build();
            favorites.save(favorite);
            TransactionLog.debug(
                    log, "Favorite added userId={} productId={}", user.getId(), productId);
        }
    }

    @Override
    @Transactional
    public void remove(String phoneNumber, Long productId) {
        User user = lockUser(phoneNumber);
        favorites.deleteByUserIdAndProductId(user.getId(), productId);
        TransactionLog.debug(
                log, "Favorite removal completed userId={} productId={}", user.getId(), productId);
    }

    private User lockUser(String phoneNumber) {
        return users.findForUpdateByPhoneNumber(phoneNumber)
                .orElseThrow(() -> new ResourceNotFoundException(MessageKeys.USER_NOT_FOUND));
    }
}
