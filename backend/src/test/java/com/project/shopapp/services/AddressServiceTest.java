package com.project.shopapp.services;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.project.shopapp.dtos.AddressRequest;
import com.project.shopapp.exceptions.ResourceNotFoundException;
import com.project.shopapp.model.User;
import com.project.shopapp.model.UserAddress;
import com.project.shopapp.repositories.UserAddressRepository;
import com.project.shopapp.repositories.UserRepository;
import com.project.shopapp.services.impl.AddressServiceImpl;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class AddressServiceTest {
    @Mock UserRepository users;
    @Mock UserAddressRepository addresses;
    private AddressService service;
    private User user;

    @BeforeEach
    void setUp() {
        service = new AddressServiceImpl(users, addresses);
        user = new User("Customer", "0900000000", "HCM", "password", null, null);
        ReflectionTestUtils.setField(user, "id", 1L);
        when(users.findForUpdateByPhoneNumber("0900000000")).thenReturn(Optional.of(user));
    }

    @Test
    void firstAddressIsDefault() {
        when(addresses.findAllByUserIdOrderByIdAsc(1L)).thenReturn(List.of());
        when(addresses.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        assertTrue(
                service.save(
                                "0900000000",
                                null,
                                new AddressRequest("Customer", "0900000000", "HCM", false))
                        .defaultAddress());
    }

    @Test
    void rejectsEditingAddressOfAnotherUser() {
        when(addresses.findAllByUserIdOrderByIdAsc(1L)).thenReturn(List.of());
        assertThrows(
                ResourceNotFoundException.class,
                () ->
                        service.save(
                                "0900000000",
                                99L,
                                new AddressRequest("Customer", "0900000000", "HCM", true)));
    }

    @Test
    void replacesDefaultWithoutLeavingTwoDefaults() {
        UserAddress first = new UserAddress(user);
        first.setDefaultAddress(true);
        UserAddress second = new UserAddress(user);
        ReflectionTestUtils.setField(first, "id", 1L);
        ReflectionTestUtils.setField(second, "id", 2L);
        when(addresses.findAllByUserIdOrderByIdAsc(1L)).thenReturn(List.of(first, second));
        when(addresses.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        service.save("0900000000", 2L, new AddressRequest("Customer", "0900000000", "HCM", true));
        assertFalse(first.isDefaultAddress());
        assertTrue(second.isDefaultAddress());
    }
}
