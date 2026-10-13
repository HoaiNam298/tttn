package com.project.shopapp.filters;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.project.shopapp.components.CustomUserDetailsService;
import com.project.shopapp.components.JwtService;
import com.project.shopapp.model.Token;
import com.project.shopapp.model.User;
import com.project.shopapp.repositories.TokenRepository;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import java.time.Instant;
import java.util.Optional;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.context.SecurityContextHolder;

class JwtAuthenticationFilterTest {
    private final JwtService jwt = mock(JwtService.class);
    private final TokenRepository tokens = mock(TokenRepository.class);
    private final CustomUserDetailsService details = mock(CustomUserDetailsService.class);
    private final JwtAuthenticationFilter filter =
            new JwtAuthenticationFilter(jwt, details, tokens);

    @AfterEach
    void cleanup() {
        MDC.clear();
        SecurityContextHolder.clearContext();
    }

    @Test
    void authenticatedUserIdIsAvailableDuringRequestAndCleanedAfterwards() throws Exception {
        User user = User.builder().id(12L).build();
        Token stored = Token.builder().user(user).expiresAt(Instant.now().plusSeconds(60)).build();
        var userDetails =
                org.springframework.security.core.userdetails.User.withUsername("test-user")
                        .password("unused")
                        .roles("USER")
                        .build();
        when(jwt.extractUsername("test-token")).thenReturn("test-user");
        when(tokens.findByToken("test-token")).thenReturn(Optional.of(stored));
        when(details.loadUserByUsername("test-user")).thenReturn(userDetails);
        when(jwt.isValid("test-token", userDetails)).thenReturn(true);
        var request = request();
        var response = new MockHttpServletResponse();
        new RequestLoggingFilter()
                .doFilter(
                        request,
                        response,
                        (req, res) ->
                                filter.doFilter(
                                        req,
                                        res,
                                        (innerReq, innerRes) -> {
                                            assertEquals("12", MDC.get("userId"));
                                            assertEquals(
                                                    response.getHeader("X-Request-ID"),
                                                    MDC.get("requestId"));
                                            assertNotNull(
                                                    SecurityContextHolder.getContext()
                                                            .getAuthentication());
                                        }));
        assertNull(MDC.get("userId"));
        assertNull(MDC.get("requestId"));
    }

    @Test
    void invalidTokenContinuesUnauthenticated() throws Exception {
        var request = request();
        var response = new MockHttpServletResponse();
        var chain = mock(FilterChain.class);
        when(jwt.extractUsername("test-token")).thenThrow(new JwtException("invalid"));
        filter.doFilter(request, response, chain);
        verify(chain).doFilter(request, response);
        verifyNoInteractions(tokens);
    }

    @Test
    void infrastructureFailureReturns500InsteadOfPretendingTokenIsInvalid() throws Exception {
        var request = request();
        var response = new MockHttpServletResponse();
        var chain = mock(FilterChain.class);
        when(jwt.extractUsername("test-token")).thenReturn("test-user");
        when(tokens.findByToken("test-token"))
                .thenThrow(new DataAccessResourceFailureException("database unavailable"));
        filter.doFilter(request, response, chain);
        assertEquals(500, response.getStatus());
        verifyNoInteractions(chain);
    }

    private MockHttpServletRequest request() {
        var request = new MockHttpServletRequest("GET", "/api/v1/orders");
        request.addHeader("Authorization", "Bearer test-token");
        return request;
    }
}
