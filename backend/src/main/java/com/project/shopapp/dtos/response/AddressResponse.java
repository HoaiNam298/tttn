package com.project.shopapp.dtos.response;

import com.project.shopapp.model.UserAddress;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AddressResponse {

    private Long id;

    private String recipientName;

    private String phoneNumber;

    private String shippingAddress;

    private boolean defaultAddress;

    public static AddressResponse from(UserAddress address) {
        return AddressResponse.builder()
                .id(address.getId())
                .recipientName(address.getRecipientName())
                .phoneNumber(address.getPhoneNumber())
                .shippingAddress(address.getShippingAddress())
                .defaultAddress(address.isDefaultAddress())
                .build();
    }
}
