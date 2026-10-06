package com.project.shopapp.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.project.shopapp.filters.JwtAuthenticationFilter;
import com.project.shopapp.model.OrderStatus;
import com.project.shopapp.services.OrderService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.Page;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(OrderController.class)
@AutoConfigureMockMvc(addFilters = false)
class OrderHistoryControllerTest {
    @Autowired MockMvc mockMvc;
    @MockBean OrderService service;
    @MockBean JwtAuthenticationFilter filter;

    @Test
    void statusFilterUsesAuthenticatedPrincipalRatherThanClientUserId() throws Exception {
        when(service.findOwnedOrders(eq("0900000000"), eq(OrderStatus.SHIPPING), any()))
                .thenReturn(Page.empty());
        mockMvc.perform(
                        get("/api/v1/orders")
                                .principal(() -> "0900000000")
                                .param("status", "SHIPPING")
                                .param("userId", "999"))
                .andExpect(status().isOk());
        verify(service).findOwnedOrders(eq("0900000000"), eq(OrderStatus.SHIPPING), any());
    }

    @Test
    void rejectsUnknownStatus() throws Exception {
        mockMvc.perform(
                        get("/api/v1/orders")
                                .principal(() -> "0900000000")
                                .param("status", "UNKNOWN"))
                .andExpect(status().isBadRequest());
    }
}
