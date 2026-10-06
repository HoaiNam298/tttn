package com.project.shopapp.services.impl;

import com.project.shopapp.dtos.AddressRequest;
import com.project.shopapp.exceptions.ResourceNotFoundException;
import com.project.shopapp.model.User;
import com.project.shopapp.model.UserAddress;
import com.project.shopapp.repositories.UserAddressRepository;
import com.project.shopapp.repositories.UserRepository;
import com.project.shopapp.responses.AddressResponse;
import com.project.shopapp.services.AddressService;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class AddressServiceImpl implements AddressService {
    private final UserRepository users;
    private final UserAddressRepository addresses;

    public AddressServiceImpl(UserRepository users, UserAddressRepository addresses) {
        this.users = users;
        this.addresses = addresses;
    }

    @Override
    public List<AddressResponse> findMine(String phoneNumber) {
        User user =
                users.findByPhoneNumber(phoneNumber)
                        .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        return addresses.findAllByUserIdOrderByIdAsc(user.getId()).stream()
                .map(AddressResponse::from)
                .toList();
    }

    @Override
    @Transactional
    public AddressResponse save(String phoneNumber, Long id, AddressRequest request) {
        User user = lockUser(phoneNumber);
        List<UserAddress> owned = addresses.findAllByUserIdOrderByIdAsc(user.getId());
        if (id == null && owned.size() >= 20) {
            throw new IllegalStateException("A user can save up to 20 addresses");
        }
        UserAddress address = id == null ? new UserAddress(user) : findOwned(owned, id);
        boolean makeDefault =
                owned.isEmpty() || request.defaultAddress() || address.isDefaultAddress();
        if (makeDefault) {
            owned.forEach(item -> item.setDefaultAddress(false));
            addresses.flush();
        }
        address.update(request.recipientName(), request.phoneNumber(), request.shippingAddress());
        address.setDefaultAddress(makeDefault);
        return AddressResponse.from(addresses.save(address));
    }

    @Override
    @Transactional
    public void delete(String phoneNumber, Long id) {
        User user = lockUser(phoneNumber);
        List<UserAddress> owned = addresses.findAllByUserIdOrderByIdAsc(user.getId());
        UserAddress target = findOwned(owned, id);
        addresses.delete(target);
        addresses.flush();
        if (target.isDefaultAddress()) {
            owned.stream()
                    .filter(item -> !item.getId().equals(id))
                    .findFirst()
                    .ifPresent(item -> item.setDefaultAddress(true));
        }
    }

    private User lockUser(String phoneNumber) {
        return users.findForUpdateByPhoneNumber(phoneNumber)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
    }

    private UserAddress findOwned(List<UserAddress> owned, Long id) {
        return owned.stream()
                .filter(item -> item.getId().equals(id))
                .findFirst()
                .orElseThrow(() -> new ResourceNotFoundException("Address not found"));
    }
}
