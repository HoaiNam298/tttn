package com.project.shopapp.controller;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import com.project.shopapp.components.CustomUserDetailsService;
import com.project.shopapp.components.JwtService;
import com.project.shopapp.configuration.SecurityConfig;
import com.project.shopapp.filters.JwtAuthenticationFilter;
import com.project.shopapp.filters.RequestLoggingFilter;
import com.project.shopapp.repositories.TokenRepository;
import com.project.shopapp.services.CategoryService;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(CategoryController.class)
@Import({SecurityConfig.class, RequestLoggingFilter.class, JwtAuthenticationFilter.class})
class RequestLoggingIntegrationTest {
    @Autowired MockMvc mvc;
    @MockBean CategoryService service;
    @MockBean JwtService jwtService;
    @MockBean CustomUserDetailsService userDetails;
    @MockBean TokenRepository tokens;

    @Test
    void requestIdMatchesBusinessLogAndQueryDoesNotAppearInRequestLog() throws Exception {
        Logger logger = (Logger) LoggerFactory.getLogger(RequestLoggingFilter.class);
        var appender =
                new ListAppender<ILoggingEvent>() {
                    @Override
                    protected void append(ILoggingEvent event) {
                        event.prepareForDeferredProcessing();
                        super.append(event);
                    }
                };
        appender.start();
        logger.addAppender(appender);
        try {
            when(service.findAll())
                    .thenAnswer(
                            invocation -> {
                                logger.info("Business event for test");
                                return List.of();
                            });
            var result =
                    mvc.perform(
                                    get("/api/v1/categories")
                                            .queryParam("secret", "do-not-log-this")
                                            .header("X-Request-ID", "untrusted-value"))
                            .andExpect(status().isOk())
                            .andExpect(header().exists("X-Request-ID"))
                            .andReturn();
            String id = result.getResponse().getHeader("X-Request-ID");
            assertNotEquals("untrusted-value", id);
            var events =
                    appender.list.stream()
                            .filter(event -> id.equals(event.getMDCPropertyMap().get("requestId")))
                            .toList();
            assertTrue(
                    events.stream()
                            .anyMatch(
                                    e ->
                                            e.getFormattedMessage()
                                                    .equals("Business event for test")));
            assertTrue(
                    events.stream().anyMatch(e -> e.getFormattedMessage().contains("status=200")));
            assertTrue(
                    events.stream()
                            .noneMatch(e -> e.getFormattedMessage().contains("do-not-log-this")));
            assertNull(MDC.get("requestId"));
            assertNull(MDC.get("userId"));
        } finally {
            logger.detachAppender(appender);
            appender.stop();
        }
    }

    @Test
    void corsExposesRequestId() throws Exception {
        when(service.findAll()).thenReturn(List.of());
        mvc.perform(get("/api/v1/categories").header("Origin", "http://localhost:4200"))
                .andExpect(status().isOk())
                .andExpect(header().string("Access-Control-Expose-Headers", "X-Request-ID"));
    }

    @Test
    void unauthorizedResponseIncludesRequestId() throws Exception {
        mvc.perform(post("/api/v1/categories"))
                .andExpect(status().isUnauthorized())
                .andExpect(header().exists("X-Request-ID"));
    }

    @Test
    @WithMockUser(roles = "USER")
    void forbiddenResponseIncludesRequestId() throws Exception {
        mvc.perform(post("/api/v1/categories"))
                .andExpect(status().isForbidden())
                .andExpect(header().exists("X-Request-ID"));
    }

    @Test
    void unexpectedErrorReturnsSafeBodyAndRequestId() throws Exception {
        when(service.findAll()).thenThrow(new NullPointerException("internal details"));
        mvc.perform(get("/api/v1/categories"))
                .andExpect(status().isInternalServerError())
                .andExpect(header().exists("X-Request-ID"))
                .andExpect(jsonPath("$.message").value("An unexpected error occurred"));
    }
}
