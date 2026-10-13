package com.project.shopapp.filters;

import static org.junit.jupiter.api.Assertions.*;

import jakarta.servlet.ServletException;
import java.util.HashSet;
import java.util.Set;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

class RequestLoggingFilterTest {
    private final RequestLoggingFilter filter = new RequestLoggingFilter();

    @AfterEach
    void clearMdc() {
        MDC.clear();
    }

    @Test
    void generatesDistinctIdsAndCleansUpBetweenRequests() throws Exception {
        Set<String> ids = new HashSet<>();
        MDC.put("unrelated", "keep");
        for (int i = 0; i < 2; i++) {
            var request = new MockHttpServletRequest("GET", "/api/v1/products");
            request.addHeader("X-Request-ID", "untrusted-client-value");
            var response = new MockHttpServletResponse();
            filter.doFilter(
                    request,
                    response,
                    (req, res) -> {
                        String id = response.getHeader("X-Request-ID");
                        assertNotNull(id);
                        assertEquals(id, MDC.get("requestId"));
                        assertNotEquals("untrusted-client-value", id);
                        ids.add(id);
                        MDC.put("userId", "12");
                    });
            assertNull(MDC.get("requestId"));
            assertNull(MDC.get("userId"));
            assertEquals("keep", MDC.get("unrelated"));
        }
        assertEquals(2, ids.size());
    }

    @Test
    void restoresExistingContextWhenChainThrows() {
        MDC.put("requestId", "outer-context");
        var request = new MockHttpServletRequest("POST", "/api/v1/orders");
        var response = new MockHttpServletResponse();
        assertThrows(
                ServletException.class,
                () ->
                        filter.doFilter(
                                request,
                                response,
                                (req, res) -> {
                                    throw new ServletException("failure");
                                }));
        assertEquals("outer-context", MDC.get("requestId"));
        assertNotEquals("outer-context", response.getHeader("X-Request-ID"));
    }
}
