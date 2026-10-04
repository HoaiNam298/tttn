package com.project.shopapp.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.project.shopapp.filters.JwtAuthenticationFilter;
import com.project.shopapp.model.OrderStatus;
import com.project.shopapp.model.PaymentMethod;
import com.project.shopapp.responses.OrderItemResponse;
import com.project.shopapp.responses.OrderResponse;
import com.project.shopapp.services.OrderService;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(OrderController.class)
@AutoConfigureMockMvc(addFilters = false)
class OrderControllerTest {
    @Autowired MockMvc mockMvc;
    @MockBean OrderService service;
    @MockBean JwtAuthenticationFilter jwtAuthenticationFilter;

    @Test
    void createsOrderForAuthenticatedUser() throws Exception {
        when(service.create(eq("0900000000"), any())).thenReturn(order());

        mockMvc.perform(
                        post("/api/v1/orders")
                                .principal(() -> "0900000000")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        """
                                        {
                                          "recipientName":"Nguyen Van A",
                                          "phoneNumber":"0900000000",
                                          "shippingAddress":"1 Nguyen Hue, HCM",
                                          "note":"",
                                          "items":[{"productId":8,"quantity":2}]
                                        }
                                        """))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", "/api/v1/orders/21"))
                .andExpect(jsonPath("$.total").value(230000));
    }

    @Test
    void rejectsEmptyCart() throws Exception {
        mockMvc.perform(
                        post("/api/v1/orders")
                                .principal(() -> "0900000000")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        """
                                        {
                                          "recipientName":"Nguyen Van A",
                                          "phoneNumber":"0900000000",
                                          "shippingAddress":"1 Nguyen Hue, HCM",
                                          "items":[]
                                        }
                                        """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.items").exists());
    }

    @Test
    void readsOnlyOwnedOrder() throws Exception {
        when(service.findOwnedOrder(21L, "0900000000")).thenReturn(order());

        mockMvc.perform(get("/api/v1/orders/21").principal(() -> "0900000000"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.orderNumber").exists());
    }

    private OrderResponse order() {
        return new OrderResponse(
                21L,
                UUID.randomUUID(),
                "Nguyen Van A",
                "0900000000",
                "1 Nguyen Hue, HCM",
                "",
                OrderStatus.PENDING,
                PaymentMethod.COD,
                new BigDecimal("200000"),
                new BigDecimal("30000"),
                new BigDecimal("230000"),
                Instant.now(),
                List.of(
                        new OrderItemResponse(
                                8L,
                                "Phone",
                                new BigDecimal("100000"),
                                2,
                                new BigDecimal("200000"))));
    }
}
