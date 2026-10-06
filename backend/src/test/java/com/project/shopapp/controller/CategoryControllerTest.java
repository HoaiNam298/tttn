package com.project.shopapp.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.project.shopapp.filters.JwtAuthenticationFilter;
import com.project.shopapp.responses.CategoryResponse;
import com.project.shopapp.services.CategoryService;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(CategoryController.class)
@AutoConfigureMockMvc(addFilters = false)
class CategoryControllerTest {
    @Autowired MockMvc mockMvc;
    @MockBean CategoryService service;
    @MockBean JwtAuthenticationFilter jwtAuthenticationFilter;

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
        mockMvc.perform(
                        post("/api/v1/categories")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"name\":\"Tablet\"}"))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", "/api/v1/categories/4"));
    }

    @Test
    void returnsFilteredCategoryPage() throws Exception {
        when(service.findPage(eq("Laptop"), any()))
                .thenReturn(
                        new PageImpl<>(
                                List.of(new CategoryResponse(1L, "Laptop")),
                                PageRequest.of(1, 5, Sort.by("id")),
                                6));
        mockMvc.perform(
                        get("/api/v1/categories/page")
                                .param("keyword", "Laptop")
                                .param("page", "1")
                                .param("size", "5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].name").value("Laptop"))
                .andExpect(jsonPath("$.totalElements").value(6))
                .andExpect(jsonPath("$.number").value(1));
        verify(service).findPage("Laptop", PageRequest.of(1, 5, Sort.by("id")));
    }
}
