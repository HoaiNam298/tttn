package com.project.shopapp.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.project.shopapp.filters.JwtAuthenticationFilter;
import com.project.shopapp.responses.CategoryResponse;
import com.project.shopapp.responses.ProductResponse;
import com.project.shopapp.services.ProductService;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.PageImpl;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(ProductController.class)
@AutoConfigureMockMvc(addFilters = false)
class ProductControllerTest {
    @Autowired MockMvc mockMvc;
    @MockBean ProductService service;
    @MockBean JwtAuthenticationFilter jwtAuthenticationFilter;

    @Test
    void searchesProducts() throws Exception {
        when(service.findAll(eq("phone"), eq(1L), any()))
                .thenReturn(new PageImpl<>(List.of(product())));

        mockMvc.perform(get("/api/v1/products").param("keyword", "phone").param("categoryId", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].name").value("Smart phone"));
    }

    @Test
    void updatesProduct() throws Exception {
        when(service.update(eq(8L), any())).thenReturn(product());

        mockMvc.perform(
                        put("/api/v1/products/8")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        """
                                        {"name":"Smart phone","price":12000000,"thumbnail":"","description":"New","categoryId":1}
                                        """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(8));
    }

    @Test
    void deletesProduct() throws Exception {
        mockMvc.perform(delete("/api/v1/products/8")).andExpect(status().isNoContent());
        verify(service).delete(8L);
    }

    private ProductResponse product() {
        return new ProductResponse(
                8L,
                "Smart phone",
                new BigDecimal("12000000"),
                "",
                "New",
                new CategoryResponse(1L, "Phone"),
                Instant.now(),
                Instant.now());
    }
}
