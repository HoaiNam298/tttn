package com.project.shopapp.services;

import com.project.shopapp.dtos.response.PaymentLinkResponse;
import com.project.shopapp.dtos.response.VnpayIpnResponse;
import org.springframework.util.MultiValueMap;

public interface PaymentService {
    PaymentLinkResponse createLink(Long orderId, String phoneNumber, String ip);

    VnpayIpnResponse receiveIpn(MultiValueMap<String, String> params);

    String returnLocation(MultiValueMap<String, String> params);
}
