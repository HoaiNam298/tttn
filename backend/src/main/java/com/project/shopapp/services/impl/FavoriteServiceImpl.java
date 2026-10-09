package com.project.shopapp.services.impl;

import com.project.shopapp.exceptions.ResourceNotFoundException;
import com.project.shopapp.model.Favorite;
import com.project.shopapp.model.User;
import com.project.shopapp.repositories.FavoriteRepository;
import com.project.shopapp.repositories.ProductRepository;
import com.project.shopapp.repositories.UserRepository;
import com.project.shopapp.responses.ProductResponse;
import com.project.shopapp.services.FavoriteService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
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
        return favorites
                .findByUserPhoneNumber(phoneNumber, bounded)
                .map(favorite -> ProductResponse.from(favorite.getProduct()));
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
            favorites.save(
                    new Favorite(
                            user,
                            products.findById(productId)
                                    .orElseThrow(
                                            () ->
                                                    new ResourceNotFoundException(
                                                            "Product not found"))));
        }
    }

    @Override
    @Transactional
    public void remove(String phoneNumber, Long productId) {
        User user = lockUser(phoneNumber);
        favorites.deleteByUserIdAndProductId(user.getId(), productId);
    }

    private User lockUser(String phoneNumber) {
        return users.findForUpdateByPhoneNumber(phoneNumber)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
    }
}
