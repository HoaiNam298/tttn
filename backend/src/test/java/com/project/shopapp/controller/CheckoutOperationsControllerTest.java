package com.project.shopapp.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.project.shopapp.filters.JwtAuthenticationFilter;
import com.project.shopapp.services.OrderService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(OrderController.class)
@AutoConfigureMockMvc(addFilters = false)
class CheckoutOperationsControllerTest {
    @Autowired MockMvc mvc;
    @MockBean OrderService service;
    @MockBean JwtAuthenticationFilter filter;

    @Test
    void quoteAndCancelUseAuthenticatedPrincipal() throws Exception {
        mvc.perform(
                        post("/api/v1/orders/quote")
                                .principal(() -> "0900000000")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        """
                {"recipientName":"Customer","phoneNumber":"0900000000","shippingAddress":"HCM",
                 "items":[{"productId":8,"quantity":1}],"shippingMethod":"EXPRESS","voucherCode":"SAVE10"}
                """))
                .andExpect(status().isOk());
        verify(service).quote(eq("0900000000"), any());
        mvc.perform(
                        post("/api/v1/orders/21/cancel")
                                .principal(() -> "0900000000")
                                .param("userId", "999"))
                .andExpect(status().isOk());
        verify(service).cancelOwnedOrder(21L, "0900000000");
    }

    @Test
    void rejectsUnknownShippingAndNullOrderLine() throws Exception {
        mvc.perform(
                        post("/api/v1/orders/quote")
                                .principal(() -> "0900000000")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        """
                {"recipientName":"Customer","phoneNumber":"0900000000","shippingAddress":"HCM",
                 "items":[{"productId":8,"quantity":1}],"shippingMethod":"INVALID"}
                """))
                .andExpect(status().isBadRequest());
        mvc.perform(
                        post("/api/v1/orders/quote")
                                .principal(() -> "0900000000")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        """
                {"recipientName":"Customer","phoneNumber":"0900000000","shippingAddress":"HCM","items":[null]}
                """))
                .andExpect(status().isBadRequest());
    }
}
