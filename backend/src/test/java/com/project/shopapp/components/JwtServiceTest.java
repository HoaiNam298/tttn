package com.project.shopapp.components;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;

class JwtServiceTest {
    private static final String SECRET =
            Base64.getEncoder()
                    .encodeToString(
                            "a-secure-test-secret-with-at-least-32-bytes"
                                    .getBytes(StandardCharsets.UTF_8));

    @Test
    void generatesValidTokenForUser() {
        JwtService service = new JwtService(SECRET, 60_000);
        UserDetails user =
                User.withUsername("0900000000").password("password").roles("USER").build();

        String token = service.generateToken(user);

        assertEquals("0900000000", service.extractUsername(token));
        assertTrue(service.isValid(token, user));
    }

    @Test
    void issuesDistinctTokensForConsecutiveRequests() {
        JwtService service = new JwtService(SECRET, 60_000);
        UserDetails user =
                User.withUsername("0900000000").password("password").roles("USER").build();

        assertNotEquals(service.generateToken(user), service.generateToken(user));
    }

    @Test
    void rejectsExpiredToken() throws InterruptedException {
        JwtService service = new JwtService(SECRET, 1);
        UserDetails user =
                User.withUsername("0900000000").password("password").roles("USER").build();
        String token = service.generateToken(user);

        Thread.sleep(5);

        assertFalse(service.isValid(token, user));
    }
}
