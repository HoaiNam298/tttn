package com.project.shopapp.services;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.project.shopapp.exceptions.ResourceNotFoundException;
import com.project.shopapp.model.Category;
import com.project.shopapp.model.Product;
import com.project.shopapp.model.User;
import com.project.shopapp.repositories.FavoriteRepository;
import com.project.shopapp.repositories.ProductRepository;
import com.project.shopapp.repositories.UserRepository;
import com.project.shopapp.services.impl.FavoriteServiceImpl;
import java.math.BigDecimal;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class FavoriteServiceTest {
    @Mock FavoriteRepository favorites;
    @Mock UserRepository users;
    @Mock ProductRepository products;
    private FavoriteService service;

    @BeforeEach
    void setup() {
        service = new FavoriteServiceImpl(favorites, users, products);
    }

    private void user() {
        User user = new User("Buyer", "0900000000", "HCM", "password", null, null);
        ReflectionTestUtils.setField(user, "id", 7L);
        when(users.findForUpdateByPhoneNumber("0900000000")).thenReturn(Optional.of(user));
    }

    @Test
    void repeatedAddDoesNotInsertAgain() {
        user();
        when(favorites.existsByUserIdAndProductId(7L, 8L)).thenReturn(true);
        service.add("0900000000", 8L);
        verify(favorites, never()).save(any());
    }

    @Test
    void addsOnlyForAuthenticatedOwner() {
        user();
        when(products.findById(8L))
                .thenReturn(
                        Optional.of(
                                new Product(
                                        "Phone", BigDecimal.TEN, "", "", new Category("Phone"))));
        service.add("0900000000", 8L);
        verify(favorites).save(any());
    }

    @Test
    void missingProductDoesNotCreateFavorite() {
        user();
        assertThrows(ResourceNotFoundException.class, () -> service.add("0900000000", 8L));
        verify(favorites, never()).save(any());
    }

    @Test
    void removeScopesDeletionToOwnerEvenOnRetry() {
        user();
        service.remove("0900000000", 8L);
        verify(favorites).deleteByUserIdAndProductId(7L, 8L);
    }
}
