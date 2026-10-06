package com.project.shopapp.responses;

import com.project.shopapp.model.UserAddress;

public record AddressResponse(
        Long id,
        String recipientName,
        String phoneNumber,
        String shippingAddress,
        boolean defaultAddress) {
    public static AddressResponse from(UserAddress address) {
        return new AddressResponse(
                address.getId(),
                address.getRecipientName(),
                address.getPhoneNumber(),
                address.getShippingAddress(),
                address.isDefaultAddress());
    }
}
