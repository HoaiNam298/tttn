package com.project.shopapp.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import com.project.shopapp.dto.CategoryResponse;
import com.project.shopapp.service.CategoryService;

@WebMvcTest(CategoryController.class)
class CategoryControllerTest {
    @Autowired MockMvc mockMvc;
    @MockBean CategoryService service;

    @Test
    void returnsCategories() throws Exception {
        when(service.findAll()).thenReturn(List.of(new CategoryResponse(1L, "Laptop")));
        mockMvc.perform(get("/api/v1/categories"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").value("Laptop"));
    }

    @Test
    void createsCategory() throws Exception {
        when(service.create(any())).thenReturn(new CategoryResponse(4L, "Tablet"));
        mockMvc.perform(post("/api/v1/categories")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Tablet\"}"))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", "/api/v1/categories/4"));
    }
}
