package com.project.shopapp.services;

import com.project.shopapp.dtos.AddressRequest;
import com.project.shopapp.responses.AddressResponse;
import java.util.List;

public interface AddressService {
    List<AddressResponse> findMine(String phoneNumber);

    AddressResponse save(String phoneNumber, Long id, AddressRequest request);

    void delete(String phoneNumber, Long id);
}
