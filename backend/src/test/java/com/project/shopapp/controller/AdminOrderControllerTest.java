package com.project.shopapp.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.project.shopapp.filters.JwtAuthenticationFilter;
import com.project.shopapp.model.OrderStatus;
import com.project.shopapp.model.PaymentMethod;
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
import org.springframework.data.domain.PageImpl;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(AdminOrderController.class)
@AutoConfigureMockMvc(addFilters = false)
class AdminOrderControllerTest {
    @Autowired MockMvc mockMvc;
    @MockBean OrderService service;
    @MockBean JwtAuthenticationFilter jwtAuthenticationFilter;

    @Test
    void filtersOrdersByStatus() throws Exception {
        when(service.findAll(eq(OrderStatus.PENDING), any())).thenReturn(new PageImpl<>(List.of()));

        mockMvc.perform(get("/api/v1/admin/orders").param("status", "PENDING"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray());

        verify(service).findAll(eq(OrderStatus.PENDING), any());
    }

    @Test
    void updatesOrderStatus() throws Exception {
        when(service.updateStatus(21L, OrderStatus.CONFIRMED)).thenReturn(order());

        mockMvc.perform(
                        patch("/api/v1/admin/orders/21/status")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"status\":\"CONFIRMED\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CONFIRMED"));
    }

    @Test
    void returnsOrderDetailForAdmin() throws Exception {
        when(service.findAdminOrder(21L)).thenReturn(order());
        mockMvc.perform(get("/api/v1/admin/orders/21"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(21));
        verify(service).findAdminOrder(21L);
    }

    @Test
    void rejectsMissingStatus() throws Exception {
        mockMvc.perform(
                        patch("/api/v1/admin/orders/21/status")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.status").exists());
    }

    private OrderResponse order() {
        return new OrderResponse(
                21L,
                UUID.randomUUID(),
                "Nguyen Van A",
                "0900000000",
                "HCM",
                "",
                OrderStatus.CONFIRMED,
                PaymentMethod.COD,
                BigDecimal.TEN,
                BigDecimal.ZERO,
                BigDecimal.TEN,
                Instant.now(),
                List.of());
    }
}
