package com.project.shopapp.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.project.shopapp.components.CustomUserDetailsService;
import com.project.shopapp.components.JwtService;
import com.project.shopapp.configuration.SecurityConfig;
import com.project.shopapp.configuration.VnpayProperties;
import com.project.shopapp.dtos.response.PaymentLinkResponse;
import com.project.shopapp.dtos.response.VnpayIpnResponse;
import com.project.shopapp.filters.JwtAuthenticationFilter;
import com.project.shopapp.repositories.TokenRepository;
import com.project.shopapp.services.PaymentService;
import java.time.Instant;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(PaymentController.class)
@Import({SecurityConfig.class, JwtAuthenticationFilter.class})
class PaymentControllerTest {
    @Autowired MockMvc mvc;
    @MockBean PaymentService service;
    @MockBean VnpayProperties properties;
    @MockBean JwtService jwt;
    @MockBean CustomUserDetailsService users;
    @MockBean TokenRepository tokens;

    @Test
    void unsignedPublicIpnReturnsProtocolErrorNotLoginPage() throws Exception {
        when(service.receiveIpn(any())).thenReturn(new VnpayIpnResponse("97", "Invalid signature"));
        mvc.perform(get("/api/v1/payments/vnpay/ipn"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.RspCode").value("97"));
    }

    @Test
    void commitFailureReturnsRetryableProtocolCode() throws Exception {
        when(service.receiveIpn(any())).thenThrow(new IllegalStateException("Simulated rollback"));
        mvc.perform(get("/api/v1/payments/vnpay/ipn"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.RspCode").value("99"));
    }

    @Test
    void linkCreationRequiresAuthentication() throws Exception {
        mvc.perform(post("/api/v1/payments/vnpay/orders/7")).andExpect(status().isUnauthorized());
    }

    @Test
    void linkIsCreatedForTheAuthenticatedPrincipal() throws Exception {
        when(service.createLink(eq(7L), eq("0900000000"), any()))
                .thenReturn(
                        new PaymentLinkResponse(
                                7L,
                                "https://sandbox.vnpayment.vn/paymentv2/vpcpay.html",
                                Instant.now().plusSeconds(900)));
        mvc.perform(post("/api/v1/payments/vnpay/orders/7").with(user("0900000000").roles("USER")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.orderId").value(7));
    }

    @Test
    void methodsArePublicAndDoNotExposeCredentials() throws Exception {
        mvc.perform(get("/api/v1/payments/methods"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.vnpayEnabled").value(false))
                .andExpect(jsonPath("$.hashSecret").doesNotExist());
    }

    @Test
    void returnIsPublicAndRedirectsToConfiguredFrontend() throws Exception {
        when(service.returnLocation(any()))
                .thenReturn("https://demo.example.com/orders/7?payment=returned");
        mvc.perform(get("/api/v1/payments/vnpay/return"))
                .andExpect(status().isFound())
                .andExpect(
                        header().string(
                                        "Location",
                                        "https://demo.example.com/orders/7?payment=returned"));
    }
}
